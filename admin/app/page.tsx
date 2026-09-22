'use client';

import { useState, useEffect } from 'react';
import {
  Plus, Edit, Trash2, Eye, Monitor, Link2,
  ChevronDown, ChevronUp, Save, X, Search,
  RefreshCw, AlertTriangle, CheckCircle,
  Users, Zap, ExternalLink
} from 'lucide-react';
import { Toaster, toast } from 'react-hot-toast';

interface Episode {
  id: string;
  episode_id: string;
  title: string;
  content_type: string;
  season_number?: number;
  episode_number?: number;
  is_active: boolean;
  thumbnail_url?: string;
  created_at: string;
  links: StreamingLink[];
}

interface StreamingLink {
  id: string;
  link_id: string;
  telegram_file_path: string;
  original_hls_url?: string;
  quality: string;
  language: string;
  is_active: boolean;
  max_concurrent_users: number;
  current_users?: number;
  total_views?: number;
}

export default function AdminDashboard() {
  const [episodes, setEpisodes] = useState<Episode[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingEpisode, setEditingEpisode] = useState<Episode | null>(null);
  const [expandedEpisodes, setExpandedEpisodes] = useState<Set<string>>(new Set());
  const [stats, setStats] = useState({ totalEpisodes: 0, totalLinks: 0, activeViewers: 0 });

  // Form state
  const [formData, setFormData] = useState({
    episode_id: '',
    title: '',
    content_type: 'episode',
    season_number: '',
    episode_number: '',
    thumbnail_url: '',
    is_active: true,
    links: [] as StreamingLink[]
  });

  const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://141.147.83.191:3000';

  const fetchData = async () => {
    try {
      const res = await fetch(`${API_URL}/api/admin/episodes`);
      if (res.ok) {
        const data = await res.json();
        setEpisodes(data.episodes || []);
        setStats({
          totalEpisodes: data.episodes?.length || 0,
          totalLinks: data.episodes?.reduce((sum: number, ep: Episode) => sum + ep.links.length, 0) || 0,
          activeViewers: data.episodes?.reduce((sum: number, ep: Episode) =>
            sum + ep.links.reduce((lsum: number, link: StreamingLink) => lsum + (link.current_users || 0), 0), 0) || 0
        });
      }
    } catch (error) {
      console.error('Fetch error:', error);
      toast.error('Failed to fetch episodes');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 30000); // Refresh every 30s
    return () => clearInterval(interval);
  }, []);

  const handleAddLink = () => {
    setFormData(prev => ({
      ...prev,
      links: [...prev.links, {
        id: crypto.randomUUID(),
        link_id: '',
        telegram_file_path: '',
        original_hls_url: '',
        quality: 'Auto',
        language: 'Hindi',
        is_active: true,
        max_concurrent_users: 250
      }]
    }));
  };

  const handleRemoveLink = (index: number) => {
    setFormData(prev => ({
      ...prev,
      links: prev.links.filter((_, i) => i !== index)
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const url = editingEpisode
        ? `${API_URL}/api/admin/episodes/${editingEpisode.id}`
        : `${API_URL}/api/admin/episodes`;

      const method = editingEpisode ? 'PUT' : 'POST';

      const payload = {
        ...formData,
        season_number: formData.season_number ? parseInt(formData.season_number) : undefined,
        episode_number: formData.episode_number ? parseInt(formData.episode_number) : undefined,
        links: formData.links.map(l => ({
          ...l,
          max_concurrent_users: 250
        }))
      };

      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        toast.success(editingEpisode ? 'Episode updated!' : 'Episode created!');
        setShowModal(false);
        setEditingEpisode(null);
        resetForm();
        fetchData();
      } else {
        const error = await res.json();
        toast.error(error.error || 'Failed to save');
      }
    } catch (error) {
      toast.error('Network error');
    }
  };

  const handleDelete = async (episodeId: string) => {
    if (!confirm('Delete this episode and all its links?')) return;

    try {
      const res = await fetch(`${API_URL}/api/admin/episodes/${episodeId}`, {
        method: 'DELETE'
      });
      if (res.ok) {
        toast.success('Episode deleted');
        fetchData();
      } else {
        toast.error('Failed to delete');
      }
    } catch {
      toast.error('Network error');
    }
  };

  const handleEdit = (episode: Episode) => {
    setEditingEpisode(episode);
    setFormData({
      episode_id: episode.episode_id,
      title: episode.title,
      content_type: episode.content_type,
      season_number: episode.season_number?.toString() || '',
      episode_number: episode.episode_number?.toString() || '',
      thumbnail_url: episode.thumbnail_url || '',
      is_active: episode.is_active,
      links: episode.links.map(l => ({
        ...l,
        max_concurrent_users: l.max_concurrent_users || 250
      }))
    });
    setShowModal(true);
  };

  const handleNew = () => {
    setEditingEpisode(null);
    resetForm();
    setShowModal(true);
  };

  const resetForm = () => {
    setFormData({
      episode_id: '',
      title: '',
      content_type: 'episode',
      season_number: '',
      episode_number: '',
      thumbnail_url: '',
      is_active: true,
      links: [{
        id: crypto.randomUUID(),
        link_id: '',
        telegram_file_path: '',
        original_hls_url: '',
        quality: 'Auto',
        language: 'Hindi',
        is_active: true,
        max_concurrent_users: 250
      }]
    });
  };

  const toggleExpand = (episodeId: string) => {
    setExpandedEpisodes(prev => {
      const next = new Set(prev);
      if (next.has(episodeId)) next.delete(episodeId);
      else next.add(episodeId);
      return next;
    });
  };

  const filteredEpisodes = episodes.filter(ep =>
    ep.episode_id.toLowerCase().includes(search.toLowerCase()) ||
    ep.title.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="min-h-screen bg-gray-900 text-gray-100 font-inter">
      {/* Header */}
      <header className="bg-gray-800/50 backdrop-blur-sm border-b border-gray-700 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 bg-gradient-to-br from-red-500 to-pink-500 rounded-lg flex items-center justify-center">
                <Zap className="w-5 h-5 text-white" />
              </div>
              <span className="text-xl font-bold bg-gradient-to-r from-red-400 to-pink-400 bg-clip-text text-transparent">
                OTT Platform Admin
              </span>
            </div>
            <div className="flex items-center gap-4">
              <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 bg-gray-800 rounded-lg text-xs">
                <Monitor className="w-3 h-3 text-green-400" />
                <span className="text-green-300">{stats.activeViewers}</span>
                <span className="text-gray-500">active viewers</span>
              </div>
              <button
                onClick={handleNew}
                className="px-4 py-2 bg-gradient-to-r from-red-500 to-pink-500 text-white rounded-lg font-medium hover:from-red-600 hover:to-pink-600 transition-all flex items-center gap-2"
              >
                <Plus className="w-4 h-4" />
                Add Episode
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Stats Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
          <StatCard title="Total Episodes" value={stats.totalEpisodes} icon={Plus} color="blue" />
          <StatCard title="Streaming Links" value={stats.totalLinks} icon={Link2} color="purple" />
          <StatCard title="Active Viewers" value={stats.activeViewers} icon={Users} color="green" />
        </div>

        {/* Search */}
        <div className="mb-6">
          <div className="relative max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 w-5 h-5" />
            <input
              type="text"
              placeholder="Search episodes..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 bg-gray-800 border border-gray-700 rounded-lg focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500/20"
            />
          </div>
        </div>

        {/* Episodes List */}
        <div className="bg-gray-800/50 border border-gray-700 rounded-xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-900/50 border-b border-gray-700">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Episode</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Type</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Links</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Active Viewers</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Status</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-gray-400 uppercase tracking-wider pr-4">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-700">
                {loading ? (
                  <tr>
                    <td colSpan={6} className="px-4 py-12 text-center">
                      <div className="flex items-center justify-center gap-3">
                        <div className="w-8 h-8 border-4 border-red-500 border-t-transparent rounded-full animate-spin" />
                        <span className="text-gray-400">Loading episodes...</span>
                      </div>
                    </td>
                  </tr>
                ) : filteredEpisodes.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-4 py-12 text-center text-gray-500">
                      No episodes found
                    </td>
                  </tr>
                ) : (
                  filteredEpisodes.map((episode) => (
                    <React.Fragment key={episode.id}>
                      <tr className="hover:bg-gray-800/50 transition-colors">
                        <td className="px-4 py-4">
                          <div>
                            <p className="font-medium">{episode.title}</p>
                            <p className="text-xs text-gray-500">ID: {episode.episode_id}</p>
                            {episode.season_number && episode.episode_number && (
                              <p className="text-xs text-gray-500">S{episode.season_number} E{episode.episode_number}</p>
                            )}
                          </div>
                        </td>
                        <td className="px-4 py-4">
                          <span className={`px-2 py-0.5 text-xs rounded-full ${
                            episode.content_type === 'movie'
                              ? 'bg-purple-500/20 text-purple-300'
                              : 'bg-blue-500/20 text-blue-300'
                          }`}>
                            {episode.content_type}
                          </span>
                        </td>
                        <td className="px-4 py-4 text-sm">{episode.links.length}</td>
                        <td className="px-4 py-4">
                          <span className="flex items-center gap-1 text-sm font-medium text-green-400">
                            <span className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
                            {episode.links.reduce((sum, l) => sum + (l.current_users || 0), 0)}
                          </span>
                        </td>
                        <td className="px-4 py-4">
                          <span className={`px-2 py-0.5 text-xs rounded-full ${
                            episode.is_active
                              ? 'bg-green-500/20 text-green-300'
                              : 'bg-red-500/20 text-red-300'
                          }`}>
                            {episode.is_active ? 'Active' : 'Inactive'}
                          </span>
                        </td>
                        <td className="px-4 py-4 text-right pr-4">
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => toggleExpand(episode.id)}
                              className="p-2 text-gray-400 hover:text-white hover:bg-gray-700 rounded-lg transition-colors"
                              title={expandedEpisodes.has(episode.id) ? 'Collapse' : 'Expand'}
                            >
                              {expandedEpisodes.has(episode.id) ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
                            </button>
                            <button
                              onClick={() => handleEdit(episode)}
                              className="p-2 text-gray-400 hover:text-blue-400 hover:bg-gray-700 rounded-lg transition-colors"
                              title="Edit"
                            >
                              <Edit className="w-5 h-5" />
                            </button>
                            <button
                              onClick={() => handleDelete(episode.id)}
                              className="p-2 text-gray-400 hover:text-red-400 hover:bg-gray-700 rounded-lg transition-colors"
                              title="Delete"
                            >
                              <Trash2 className="w-5 h-5" />
                            </button>
                          </div>
                        </td                      </tr>

                      {/* Expanded Links Row */}
                      {expandedEpisodes.has(episode.id) && (
                        <tr className="bg-gray-900/50">
                          <td colSpan={6} className="px-4 py-4">
                            <div className="ml-4 border-l-2 border-gray-700 pl-4 space-y-3">
                              {episode.links.map((link, index) => (
                                <LinkRow
                                  key={link.id}
                                  link={link}
                                  index={index + 1}
                                  onTestStream={() => testStream(link)}
                                />
                              ))}
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  ))}
                )}
              </tbody>
            </table>
          </div>
        </div>
      </main>

      {/* Add/Edit Modal */}
      {showModal && (
        <Modal
          isOpen={showModal}
          onClose={() => { setShowModal(false); setEditingEpisode(null); resetForm(); }}
          title={editingEpisode ? 'Edit Episode' : 'Add New Episode'}
        >
          <form onSubmit={handleSubmit} className="space-y-4 max-h-[70vh] overflow-y-auto pr-2">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Episode ID"
                name="episode_id"
                value={formData.episode_id}
                onChange={(e) => setFormData({...formData, episode_id: e.target.value})}
                required={!editingEpisode}
                disabled={!!editingEpisode}
                placeholder="e.g., ep_001"
              />
              <Input
                label="Title"
                name="title"
                value={formData.title}
                onChange={(e) => setFormData({...formData, title: e.target.value})}
                required
                placeholder="Episode title"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
              <Select
                label="Content Type"
                name="content_type"
                value={formData.content_type}
                onChange={(e) => setFormData({...formData, content_type: e.target.value})}
                options={[
                  { value: 'movie', label: 'Movie' },
                  { value: 'episode', label: 'TV Episode' },
                  { value: 'live', label: 'Live Stream' }
                ]}
              />
              <Input
                label="Season"
                name="season_number"
                type="number"
                value={formData.season_number}
                onChange={(e) => setFormData({...formData, season_number: e.target.value})}
                placeholder="1"
              />
              <Input
                label="Episode"
                name="episode_number"
                type="number"
                value={formData.episode_number}
                onChange={(e) => setFormData({...formData, episode_number: e.target.value})}
                placeholder="1"
              />
              <div className="flex items-end">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={formData.is_active}
                    onChange={(e) => setFormData({...formData, is_active: e.target.checked})}
                    className="w-4 h-4 text-red-500 border-gray-600 rounded focus:ring-red-500"
                  />
                  <span className="text-sm">Active</span>
                </label>
              </div>
            </div>

            <Input
              label="Thumbnail URL"
              name="thumbnail_url"
              value={formData.thumbnail_url}
              onChange={(e) => setFormData({...formData, thumbnail_url: e.target.value})}
              placeholder="https://example.com/thumbnail.jpg"
            />

            {/* Links Section */}
            <div className="border-t border-gray-700 pt-4">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">Streaming Links (Max 250 users each)</h3>
                <button
                  type="button"
                  onClick={handleAddLink}
                  className="px-3 py-1.5 text-sm bg-gradient-to-r from-red-500 to-pink-500 text-white rounded-lg hover:from-red-600 hover:to-pink-600 flex items-center gap-1.5"
                >
                  <Plus className="w-4 h-4" />
                  Add Link
                </button>
              </div>

              <div className="space-y-3 max-h-60 overflow-y-auto">
                {formData.links.map((link, index) => (
                  <LinkFormRow
                    key={link.id}
                    link={link}
                    index={index + 1}
                    onChange={(updates) => {
                      setFormData(prev => ({
                        ...prev,
                        links: prev.links.map((l, i) => i === index ? { ...l, ...updates } : l)
                      }));
                    }}
                    onRemove={() => formData.links.length > 1 && handleRemoveLink(index)}
                    disabled={formData.links.length === 1}
                  />
                ))}
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-gray-700">
              <button
                type="button"
                onClick={() => { setShowModal(false); setEditingEpisode(null); resetForm(); }}
                className="px-6 py-2 text-gray-300 hover:text-white bg-gray-800 hover:bg-gray-700 rounded-lg transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-6 py-2 bg-gradient-to-r from-red-500 to-pink-500 text-white rounded-lg font-medium hover:from-red-600 hover:to-pink-600 transition-all flex items-center gap-2"
              >
                <Save className="w-4 h-4" />
                {editingEpisode ? 'Update' : 'Create'} Episode
              </button>
            </div>
          </form>
        </Modal>
      )>

      <Toaster
        position="top-right"
        toastOptions={{
          duration: 3000,
          style: { background: '#1f2937', color: '#f3f4f6' },
          success: { iconTheme: { primary: '#22c55e', secondary: '#fff' } },
          error: { iconTheme: { primary: '#ef4444', secondary: '#fff' } }
        }}
      />
    </div>
  );
}

// StatCard Component
function StatCard({ title, value, icon: Icon, color }: {
  title: string;
  value: number;
  icon: React.FC<{className?: string}>;
  color: string
}) {
  const colors = {
    blue: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
    purple: 'bg-purple-500/20 text-purple-400 border-purple-500/30',
    green: 'bg-green-500/20 text-green-400 border-green-500/30'
  };

  return (
    <div className={`p-5 bg-gray-800/50 border rounded-xl ${colors[color as keyof typeof colors] || colors.blue}`}>
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-400">{title}</p>
          <p className="text-3xl font-bold mt-1">{value}</p>
        </div>
        <Icon className="w-12 h-12 opacity-50" />
      </div>
    </div>
  );
}

// Input Component
function Input({ label, name, value, onChange, type = 'text', required, disabled, placeholder }: any) {
  return (
    <div className={disabled ? 'opacity-50' : ''}>
      <label className="block text-sm font-medium text-gray-300 mb-1.5">{label} {required && <span className="text-red-400">*</span>}</label>
      <input
        type={type}
        value={value}
        onChange={onChange}
        disabled={disabled}
        placeholder={placeholder}
        className="w-full px-4 py-2.5 bg-gray-800 border border-gray-700 rounded-lg focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500/20 disabled:bg-gray-900 disabled:cursor-not-allowed"
      />
    </div>
  );
}

// Select Component
function Select({ label, name, value, onChange, options }: any) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-300 mb-1.5">{label}</label>
      <select
        value={value}
        onChange={onChange}
        className="w-full px-4 py-2.5 bg-gray-800 border border-gray-700 rounded-lg focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500/20"
      >
        {options.map((opt: any) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    </div>
  );
}

// LinkFormRow Component
function LinkFormRow({ link, index, onChange, onRemove, disabled }: any) {
  return (
    <div className="bg-gray-900/50 border border-gray-700 rounded-lg p-4 space-y-3">
      <div className="flex items-center justify-between">
        <span className="font-medium">Link #{index}</span>
        <button
          type="button"
          onClick={onRemove}
          disabled={disabled}
          className="p-1.5 text-gray-400 hover:text-red-400 hover:bg-red-500/10 rounded transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <Input
          label="Link ID"
          name="link_id"
          value={link.link_id}
          onChange={(e) => onChange({ link_id: e.target.value })}
          placeholder="e.g., ep_001_link_1"
        />
        <Input
          label="Quality"
          name="quality"
          value={link.quality}
          onChange={(e) => onChange({ quality: e.target.value })}
          placeholder="Auto"
        />
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <Input
          label="Language"
          name="language"
          value={link.language}
          onChange={(e) => onChange({ language: e.target.value })}
          placeholder="Hindi"
        />
        <Select
          label="Max Users"
          name="max_concurrent_users"
          value={link.max_concurrent_users?.toString() || '250'}
          onChange={(e) => onChange({ max_concurrent_users: parseInt(e.target.value) })}
          options={[
            { value: '100', label: '100 users' },
            { value: '200', label: '200 users' },
            { value: '250', label: '250 users (default)' },
            { value: '500', label: '500 users' }
          ]}
        />
      </div>

      <Input
        label="Telegram File Path / HLS URL"
        name="telegram_file_path"
        value={link.telegram_file_path}
        onChange={(e) => onChange({ telegram_file_path: e.target.value })}
        placeholder="/path/to/file.m3u8 or https://..."
        required
      />

      <div className="flex items-center gap-3 pt-2">
        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={link.is_active}
            onChange={(e) => onChange({ is_active: e.target.checked })}
            className="w-4 h-4 text-red-500 border-gray-600 rounded focus:ring-red-500"
          />
          <span className="text-sm">Active Link</span>
        </label>

        {link.current_users !== undefined && (
          <span className="ml-4 px-2 py-0.5 text-xs bg-blue-500/20 text-blue-300 rounded">
            {link.current_users}/{link.max_concurrent_users} active
          </span>
        )}

        {link.total_views !== undefined && (
          <span className="px-2 py-0.5 text-xs bg-purple-500/20 text-purple-300 rounded">
            {link.total_views} total views
          </span>
        )}
      </div>
    </div>
  );
}

// LinkRow Component for expanded view
function LinkRow({ link, index, onTestStream }: any) {
  return (
    <div className="bg-gray-900/50 border border-gray-700 rounded-lg p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
      <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 flex-1">
        <span className="px-2 py-1 bg-gray-700 rounded text-sm font-mono text-gray-300 min-w-[80px] text-center">#{index}</span>
        <div className="space-y-1">
          <p className="font-medium">{link.link_id}</p>
          <p className="text-xs text-gray-500 truncate max-w-xs">{link.telegram_file_path}</p>
        </div>
        <div className="flex items-center gap-2 flex-wrap">
          <span className="px-2 py-0.5 text-xs bg-blue-500/20 text-blue-300 rounded">{link.quality}</span>
          <span className="px-2 py-0.5 text-xs bg-purple-500/20 text-purple-300 rounded">{link.language}</span>
          <span className={`px-2 py-0.5 text-xs rounded ${
            link.is_active ? 'bg-green-500/20 text-green-300' : 'bg-red-500/20 text-red-300'
          }`}>
            {link.is_active ? 'Active' : 'Inactive'}
          </span>
        </div>
      </div>

      <div className="flex items-center gap-4">
        <div className="flex items-center gap-3 text-sm text-gray-400">
          <span className="flex items-center gap-1">
            <Users className="w-3 h-3 text-green-400" />
            {link.current_users || 0}/{link.max_concurrent_users}
          </span>
          <span className="flex items-center gap-1">
            <Eye className="w-3 h-3 text-purple-400" />
            {link.total_views || 0}
          </span>
        </div>
        <button
          onClick={onTestStream}
          className="px-3 py-1.5 text-xs bg-gradient-to-r from-blue-500 to-purple-500 text-white rounded-lg hover:from-blue-600 hover:to-purple-600 flex items-center gap-1.5"
        >
          <ExternalLink className="w-3.5 h-3.5" />
          Test Stream
        </button>
      </div>
    </div>
  );
}

// Modal Component
function Modal({ isOpen, onClose, title, children }: any) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
      <div className="bg-gray-800 border border-gray-700 rounded-2xl w-full max-w-3xl max-h-[85vh] overflow-hidden animate-slide-up">
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-700">
          <h2 className="text-xl font-semibold">{title}</h2>
          <button
            onClick={onClose}
            className="p-2 text-gray-400 hover:text-white hover:bg-gray-700 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="p-6">{children}</div>
      </div>
    </div>
  );
}

export default AdminDashboard;