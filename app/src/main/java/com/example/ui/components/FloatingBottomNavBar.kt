package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme

enum class NavigationTab(val title: String, val icon: ImageVector, val tag: String) {
    CHATS("Chats", Icons.AutoMirrored.Filled.Chat, "nav_tab_chats"),
    CALLS("Calls", Icons.Default.Call, "nav_tab_calls"),
    STORIES("Stories", Icons.Default.AutoAwesome, "nav_tab_stories"),
    SETTINGS("You", Icons.Default.Person, "nav_tab_settings")
}

@Composable
fun FloatingBottomNavBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    unreadChatsCount: Int = 0,
    missedCallsCount: Int = 0,
    hasNewStories: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // iOS-style Glassmorphism Pill Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = if (colors.isDark) 20.dp else 12.dp,
                    shape = RoundedCornerShape(34.dp),
                    spotColor = if (colors.isDark) Color.Black.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.16f),
                    ambientColor = if (colors.isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.08f)
                ),
            shape = RoundedCornerShape(34.dp),
            color = colors.navBarBg,
            border = BorderStroke(1.dp, colors.navBarBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab

                    val animatedBgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (colors.isDark) Color(0xFF262626) else Color(0xFFEFEFEF)
                        } else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab_bg_color"
                    )

                    val animatedBorderColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (colors.isDark) Color(0xFF363636) else Color(0xFFDBDBDB)
                        } else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab_border_color"
                    )

                    val animatedContentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (colors.isDark) Color.White else Color(0xFF111111)
                        } else colors.textMuted,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab_content_color"
                    )

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = animatedBgColor,
                        border = BorderStroke(1.dp, animatedBorderColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onTabSelected(tab)
                            }
                            .testTag(tab.tag)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = animatedContentColor,
                                    modifier = Modifier.size(22.dp)
                                )

                                // Badges
                                when (tab) {
                                    NavigationTab.CHATS -> {
                                        if (unreadChatsCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = 10.dp, y = (-6).dp)
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (unreadChatsCount > 9) "9+" else unreadChatsCount.toString(),
                                                    color = Color.Black,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    NavigationTab.CALLS -> {
                                        if (missedCallsCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = 10.dp, y = (-6).dp)
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (missedCallsCount > 9) "9+" else missedCallsCount.toString(),
                                                    color = Color.Black,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    NavigationTab.STORIES -> {
                                        if (hasNewStories) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = 9.dp, y = (-6).dp)
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                        }
                                    }
                                    NavigationTab.SETTINGS -> {}
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = tab.title,
                                color = animatedContentColor,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
