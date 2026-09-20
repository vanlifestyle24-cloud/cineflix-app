package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.SessionManager

data class VipPlan(
    val id: String,
    val title: String,
    val price: String,
    val period: String,
    val discountBadge: String? = null,
    val isPopular: Boolean = false,
    val features: List<String>
)

@Composable
fun PremiumScreen(
    sessionManager: SessionManager,
    onNavigateToHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isVipActive by sessionManager.isVipActive.collectAsState()
    val currentVipPlan by sessionManager.vipPlan.collectAsState()

    val plans = remember {
        listOf(
            VipPlan(
                id = "plan_month",
                title = "1 Month VIP",
                price = "₹299",
                period = "per month",
                features = listOf("1080p Full HD", "Zero Ads", "2 Screens", "Standard Speed")
            ),
            VipPlan(
                id = "plan_year",
                title = "1 Year Ultra VIP",
                price = "₹1,499",
                period = "per year (₹124/mo)",
                discountBadge = "SAVE 60%",
                isPopular = true,
                features = listOf("4K Ultra HD + HDR10+", "Dolby Atmos 7.1", "4 Screens", "Unlimited Cloud Downloads", "Priority Edge CDN")
            ),
            VipPlan(
                id = "plan_lifetime",
                title = "Lifetime Master VIP",
                price = "₹3,999",
                period = "one-time payment",
                discountBadge = "LIFETIME",
                features = listOf("Lifetime Permanent VIP", "All Current & Future Perks", "VIP Premium Club Access", "Dedicated 24/7 Support")
            )
        )
    }

    var selectedPlanId by remember { mutableStateOf("plan_year") }
    var promoCodeInput by remember { mutableStateOf("") }
    var promoFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var promoSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .testTag("premium_screen")
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF241A0B), // Warm luxury dark gold glow
                        Color(0xFF14131A),
                        Color(0xFF0E0D14)
                    )
                )
            )
    ) {
        // Sticky Header with status bar padding
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xF5171520),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFFD54F), Color(0xFFFFA000))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "VIP",
                            tint = Color(0xFF2A1B00),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CINEFLIX VIP",
                            color = Color(0xFFFFD54F),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isVipActive) "Active Plan: $currentVipPlan" else "Unlock 4K Streaming & Cloud Downloads",
                            color = Color(0xFFA6A6BA),
                            fontSize = 11.sp
                        )
                    }
                }

                if (isVipActive) {
                    Surface(
                        color = Color(0x3300E676),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E676))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "VIP ACTIVE",
                                color = Color(0xFF00E676),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Scrollable Body
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. VIP Membership Hero Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    if (isVipActive) listOf(Color(0xFF422F0F), Color(0xFF7A5818), Color(0xFFB38628))
                                    else listOf(Color(0xFF2C2417), Color(0xFF3B2F1B), Color(0xFF4C3E20))
                                )
                            )
                            .border(
                                1.5.dp,
                                Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFB300))),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isVipActive) "👑 VIP MEMBERSHIP ACTIVE" else "UPGRADE TO VIP ULTRA",
                                        color = Color(0xFFFFE082),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isVipActive) currentVipPlan else "Unlimited 4K Movies & Series",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isVipActive) "Valid through Sep 2027 • All VIP servers unlocked"
                                        else "Direct high-speed stream links from cloud CDN, zero buffering & ad-free.",
                                        color = Color(0xFFE0D8C3),
                                        fontSize = 12.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Diamond,
                                        contentDescription = "Diamond",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Features Pills Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MiniPill(text = "4K HDR")
                                MiniPill(text = "0 Ads")
                                MiniPill(text = "50 MB/s Cloud")
                                MiniPill(text = "Offline Decrypt")
                            }
                        }
                    }
                }
            }

            // 2. Select VIP Plan Section
            item {
                Text(
                    text = "Choose Your Membership Plan",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    plans.forEach { plan ->
                        val isSelected = selectedPlanId == plan.id
                        val borderColor by animateColorAsState(
                            if (isSelected) Color(0xFFFFD54F) else Color(0x33FFFFFF)
                        )
                        val bgGradient = if (isSelected) {
                            Brush.horizontalGradient(listOf(Color(0xFF2E2211), Color(0xFF1E1710)))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFF1A1924), Color(0xFF14131D)))
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlanId = plan.id },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bgGradient)
                                    .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Radio indicator
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    2.dp,
                                                    if (isSelected) Color(0xFFFFD54F) else Color(0xFF6E6E80),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFFD54F))
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = plan.title,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (plan.discountBadge != null) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        color = Color(0xFFE53935),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = plan.discountBadge,
                                                            color = Color.White,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = plan.features.first(),
                                                color = Color(0xFFA6A6BC),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = plan.price,
                                            color = Color(0xFFFFD54F),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = plan.period,
                                            color = Color(0xFF88889C),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Instant Activate VIP Button
            item {
                val chosenPlan = plans.find { it.id == selectedPlanId } ?: plans[1]
                Button(
                    onClick = {
                        sessionManager.activateVip(chosenPlan.title)
                        promoFeedbackMessage = "🎉 Successfully activated ${chosenPlan.title}! All 4K titles unlocked."
                        promoSuccess = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_activate_vip"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color(0xFF241500)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isVipActive) "Update to ${chosenPlan.title}" else "Activate ${chosenPlan.title} (${chosenPlan.price})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. Redeem Promo Code / Voucher Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161520)),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = "Coupon",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Have a Voucher or Gift Card?",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Input + Redeem button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF22212E))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (promoCodeInput.isEmpty()) {
                                    Text(
                                        text = "Enter code (e.g. VIP2026)",
                                        color = Color(0xFF88889C),
                                        fontSize = 13.sp
                                    )
                                }
                                BasicTextField(
                                    value = promoCodeInput,
                                    onValueChange = { promoCodeInput = it },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(Color(0xFFFFD54F)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val (success, msg) = sessionManager.redeemPromoCode(promoCodeInput)
                                    promoFeedbackMessage = msg
                                    promoSuccess = success
                                    if (success) promoCodeInput = ""
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E5FF),
                                    contentColor = Color(0xFF00222B)
                                ),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text("Redeem", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Quick suggestion pills
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Try:", color = Color(0xFF88889C), fontSize = 11.sp)
                            listOf("VIP2026", "FREE4K", "STUDENT100").forEach { code ->
                                Surface(
                                    color = Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable {
                                        promoCodeInput = code
                                    }
                                ) {
                                    Text(
                                        text = code,
                                        color = Color(0xFFFFD54F),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Promo feedback message
                        AnimatedVisibility(visible = promoFeedbackMessage != null) {
                            promoFeedbackMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = if (promoSuccess) Color(0x3300E676) else Color(0x33FF5252),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (promoSuccess) Color(0xFF00E676) else Color(0xFFFF5252))
                                ) {
                                    Text(
                                        text = msg,
                                        color = if (promoSuccess) Color(0xFF00E676) else Color(0xFFFF8A80),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. VIP Benefits Breakdown Grid
            item {
                Text(
                    text = "What You Get with VIP",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VipPerkCard(
                            icon = Icons.Default.HighQuality,
                            title = "4K Ultra HD & HDR",
                            desc = "Crystal clear cinema visuals with HDR10+ support.",
                            modifier = Modifier.weight(1f)
                        )
                        VipPerkCard(
                            icon = Icons.Default.CheckCircle,
                            title = "100% Ad-Free",
                            desc = "Zero popup banners, zero commercial interruptions.",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VipPerkCard(
                            icon = Icons.Default.Speed,
                            title = "High-Speed CDN",
                            desc = "Direct supercharged 50+ MB/s dedicated bandwidth.",
                            modifier = Modifier.weight(1f)
                        )
                        VipPerkCard(
                            icon = Icons.Default.Download,
                            title = "Unlimited Downloads",
                            desc = "Save full movies offline to watch on flights or travel.",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VipPerkCard(
                            icon = Icons.Default.VolumeUp,
                            title = "Dolby Atmos 7.1",
                            desc = "Immersive multi-channel surround sound staging.",
                            modifier = Modifier.weight(1f)
                        )
                        VipPerkCard(
                            icon = Icons.Default.Tv,
                            title = "4 Simultaneous Screens",
                            desc = "Stream on Phone, Tablet, PC and Smart TV together.",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPill(text: String) {
    Surface(
        color = Color(0x33000000),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, Color(0x66FFD54F))
    ) {
        Text(
            text = text,
            color = Color(0xFFFFECB3),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun VipPerkCard(
    icon: ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191824)),
        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFD54F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                color = Color(0xFFA6A6BA),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
