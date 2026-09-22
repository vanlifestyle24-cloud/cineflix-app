/**
 * OTT Platform Backend Proxy Server
 * Fetches HLS chunks from Telegram private channels and streams to users
 * All Telegram traffic goes through this backend - users need no VPN
 */

require('dotenv').config();
const express = require('express');
const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const rateLimit = require('express-rate-limit');
const helmet = require('helmet');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;

// Security middleware
app.use(helmet());
app.use(cors({ origin: process.env.ALLOWED_ORIGINS || '*' }));
app.use(express.json({ limit: '100kb' }));

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 100,
  message: { error: 'Too many requests from this IP, please try again later.' }
});
app.use('/api/', limiter);

// ==========================================
// DATABASE SIMULATION (Replace with PostgreSQL/MongoDB in production)
// ==========================================

const episodesDB = new Map();
const linkStats = new Map();

// ==========================================
// HELPER: Fetch HLS playlist from Telegram
// ==========================================

function fetchHlsPlaylist(telegramFilePath) {
  return new Promise((resolve, reject) => {
    const url = `https://appassets.androidplatform.net${telegramFilePath}`;

    https.get(url, { timeout: 30000 }, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        if (res.statusCode === 200) {
          resolve(data);
        } else {
          reject(new Error(`Telegram API returned ${res.statusCode}`));
        }
      });
    }).on('error', (e) => {
      reject(new Error(`Error fetching from Telegram: ${e.message}`));
    });
  });
}

// ==========================================
// HELPER: Fetch a single TS chunk
// ==========================================

function fetchTsChunk(segmentUrl) {
  return new Promise((resolve, reject) => {
    const isHttps = segmentUrl.startsWith('https');
    const client = isHttps ? https : http;

    const req = client.get(segmentUrl, (res) => {
      const chunks = [];
      res.on('data', (chunk) => chunks.push(chunk));
      res.on('end', () => {
        resolve(Buffer.concat(chunks));
      });
    });

    req.on('error', (e) => {
      reject(new Error(`Chunk fetch error: ${e.message}`));
    });

    req.setTimeout(10000, () => {
      req.destroy();
      reject(new Error('Chunk fetch timeout'));
    });
  });
}

// ==========================================
// HELPER: Rewrite HLS playlist with proxy URLs
// ==========================================

function rewriteHlsPlaylist(originalPlaylist, baseUrl, episodeId, linkId) {
  const lines = originalPlaylist.split('\n');
  const rewritten = lines.map(line => {
    if (line.endsWith('.ts') || line.endsWith('.m3u8')) {
      // Segment or sub-playlist - rewrite to go through our proxy
      return `${baseUrl}/api/stream/segment/${episodeId}/${linkId}?segment=${encodeURIComponent(line.trim())}`;
    }
    return line;
  });
  return rewritten.join('\n');
}

// ==========================================
// API: Get episode streaming links (Smart Routing)
// ==========================================

app.get('/api/episodes/:episodeId/links', (req, res) => {
  const { episodeId } = req.params;
  const episode = episodesDB.get(episodeId);

  if (!episode) {
    return res.status(404).json({ error: 'Episode not found' });
  }

  // Check which links have capacity (< 250 active users)
  const availableLinks = episode.links.map(link => {
    const stats = linkStats.get(link.id) || { activeUsers: 0 };
    return {
      ...link,
      canStream: stats.activeUsers < 250,
      currentUsers: stats.activeUsers
    };
  }).filter(l => l.canStream);

  if (availableLinks.length === 0) {
    return res.status(503).json({
      error: 'All links at capacity (250 users each)',
      fallback: 'Please try again in a few minutes'
    });
  }

  // Sort by fewest current users (smart routing)
  availableLinks.sort((a, b) => a.currentUsers - b.currentUsers);

  // Increment user count on first available link
  const selectedLink = availableLinks[0];
  const stats = linkStats.get(selectedLink.id) || { activeUsers: 0 };
  stats.activeUsers++;
  linkStats.set(selectedLink.id, stats);

  // Track in episode
  if (!episode.activeUsers.has(selectedLink.id)) {
    episode.activeUsers.set(selectedLink.id, 0);
  }
  episode.activeUsers.set(selectedLink.id,
    episode.activeUsers.get(selectedLink.id) + 1
  );

  res.json({
    success: true,
    link: selectedLink,
    message: `Streaming from link with ${selectedLink.currentUsers} active users`
  });
});

// ==========================================
// API: Release user slot when done
// ==========================================

app.post('/api/episodes/:episodeId/release', (req, res) => {
  const { episodeId } = req.params;
  const { linkId } = req.body;

  const episode = episodesDB.get(episodeId);
  if (!episode) {
    return res.status(404).json({ error: 'Episode not found' });
  }

  // Decrement link user count
  if (linkStats.has(linkId)) {
    linkStats.set(linkId, {
      ...linkStats.get(linkId),
      activeUsers: Math.max(0, linkStats.get(linkId).activeUsers - 1)
    });
  }

  // Decrement episode user count
  if (episode.activeUsers.has(linkId)) {
    episode.activeUsers.set(linkId, Math.max(0, episode.activeUsers.get(linkId) - 1));
  }

  res.json({ success: true, message: 'User slot released' });
});

// ==========================================
// API: Admin - Add episode with multiple links
// ==========================================

app.post('/api/admin/episodes', (req, res) => {
  const { episodeId, title, thumbnail, links } = req.body;

  if (!episodeId || !links || !Array.isArray(links) || links.length === 0) {
    return res.status(400).json({ error: 'episodeId and links array required' });
  }

  const newLinks = links.map((link, index) => ({
    id: `${episodeId}_link_${index + 1}`,
    url: link.url, // Telegram file path or direct HLS URL
    quality: link.quality || 'Auto',
    language: link.language || 'Hindi',
    isActive: link.isActive !== false
  }));

  episodesDB.set(episodeId, {
    id: episodeId,
    title: title || `Episode ${episodeId}`,
    thumbnail: thumbnail || '',
    links: newLinks,
    activeUsers: new Map(),
    createdAt: new Date().toISOString()
  });

  // Initialize link stats
  newLinks.forEach(link => {
    if (!linkStats.has(link.id)) {
      linkStats.set(link.id, { activeUsers: 0, totalViews: 0 });
    }
  });

  res.json({ success: true, episode: episodesDB.get(episodeId) });
});

// ==========================================
// API: Admin - Update episode links
// ==========================================

app.put('/api/admin/episodes/:episodeId/links', (req, res) => {
  const { episodeId } = req.params;
  const { links } = req.body;

  const episode = episodesDB.get(episodeId);
  if (!episode) {
    return res.status(404).json({ error: 'Episode not found' });
  }

  // Preserve active user counts
  const activeUsers = episode.activeUsers;

  episode.links = links.map((link, index) => ({
    id: link.id || `${episodeId}_link_${index + 1}`,
    url: link.url,
    quality: link.quality || 'Auto',
    language: link.language || 'Hindi',
    isActive: link.isActive !== false
  }));

  episode.activeUsers = activeUsers;
  episodesDB.set(episodeId, episode);

  // Initialize new link stats
  episode.links.forEach(link => {
    if (!linkStats.has(link.id)) {
      linkStats.set(link.id, { activeUsers: 0, totalViews: 0 });
    }
  });

  res.json({ success: true, episode });
});

// ==========================================
// API: Admin - Delete episode
// ==========================================

app.delete('/api/admin/episodes/:episodeId', (req, res) => {
  const { episodeId } = req.params;
  const episode = episodesDB.get(episodeId);

  if (!episode) {
    return res.status(404).json({ error: 'Episode not found' });
  }

  // Clean up link stats
  episode.links.forEach(link => {
    linkStats.delete(link.id);
  });

  episodesDB.delete(episodeId);
  res.json({ success: true, message: 'Episode deleted' });
});

// ==========================================
// API: Admin - Get all episodes with stats
// ==========================================

app.get('/api/admin/episodes', (req, res) => {
  const episodes = Array.from(episodesDB.values()).map(ep => ({
    ...ep,
    linkStats: ep.links.map(l => ({
      id: l.id,
      activeUsers: linkStats.get(l.id)?.activeUsers || 0,
      totalViews: linkStats.get(l.id)?.totalViews || 0,
      isActive: l.isActive
    }))
  }));

  res.json({ success: true, episodes });
});

// ==========================================
// API: Stream HLS Playlist (rewritten for proxy)
// ==========================================

app.get('/api/stream/playlist/:episodeId/:linkId', async (req, res) => {
  const { episodeId, linkId } = req.params;
  const episode = episodesDB.get(episodeId);

  if (!episode) {
    return res.status(404).json({ error: 'Episode not found' });
  }

  const link = episode.links.find(l => l.id === linkId);
  if (!link) {
    return res.status(404).json({ error: 'Link not found' });
  }

  try {
    const originalPlaylist = await fetchHlsPlaylist(link.url);
    const baseUrl = `${req.protocol}://${req.get('host')}`;
    const rewrittenPlaylist = rewriteHlsPlaylist(originalPlaylist, baseUrl, episodeId, linkId);

    res.set({
      'Content-Type': 'application/vnd.apple.mpegurl',
      'Cache-Control': 'no-cache, no-store, must-revalidate',
      'Access-Control-Allow-Origin': '*'
    });

    res.send(rewrittenPlaylist);
  } catch (error) {
    console.error('Playlist fetch error:', error);
    res.status(502).json({ error: 'Failed to fetch playlist from source' });
  }
});

// ==========================================
// API: Stream TS Segment (proxy through backend)
// ==========================================

app.get('/api/stream/segment/:episodeId/:linkId', async (req, res) => {
  const { episodeId, linkId } = req.params;
  const { segment } = req.query;

  if (!segment) {
    return res.status(400).json({ error: 'Segment parameter required' });
  }

  const episode = episodesDB.get(episodeId);
  if (!episode) {
    return res.status(404).json({ error: 'Episode not found' });
  }

  const link = episode.links.find(l => l.id === linkId);
  if (!link) {
    return res.status(404).json({ error: 'Link not found' });
  }

  try {
    // Build full segment URL
    const segmentUrl = new URL(segment, link.url).toString();
    const chunk = await fetchTsChunk(segmentUrl);

    // Update view stats
    if (linkStats.has(linkId)) {
      linkStats.set(linkId, {
        ...linkStats.get(linkId),
        totalViews: linkStats.get(linkId).totalViews + 1
      });
    }

    res.set({
      'Content-Type': 'video/MP2T',
      'Cache-Control': 'public, max-age=31536000',
      'Access-Control-Allow-Origin': '*',
      'Accept-Ranges': 'bytes',
      'Content-Length': chunk.length
    });

    res.send(chunk);
  } catch (error) {
    console.error('Segment fetch error:', error);
    res.status(502).json({ error: 'Failed to fetch segment from source' });
  }
});

// ==========================================
// Health check
// ==========================================

app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    activeEpisodes: episodesDB.size,
    activeLinks: linkStats.size
  });
});

// ==========================================
// Start server
// ==========================================

if (require.main === module) {
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 OTT Proxy Server running on port ${PORT}`);
    console.log(`📡 Proxy endpoints:`);
    console.log(`   GET /api/episodes/:id/links - Get smart-routed streaming link`);
    console.log(`   GET /api/stream/playlist/:id/:linkId - HLS playlist (proxied)`);
    console.log(`   GET /api/stream/segment/:id/:linkId - TS chunks (proxied)`);
    console.log(`   Admin: GET/POST/PUT/DELETE /api/admin/episodes`);
  });
}

module.exports = app;