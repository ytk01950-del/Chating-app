package com.example

import com.example.service.SupabaseConfigManager
import com.example.service.SupabaseStorageService
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Supabase storage configuration, URL formatting, and validation.
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testSupabaseConfigManager_formatUrl() {
        assertEquals("https://xyz.supabase.co", SupabaseConfigManager.formatUrl("xyz.supabase.co"))
        assertEquals("https://xyz.supabase.co", SupabaseConfigManager.formatUrl("https://xyz.supabase.co/"))
        assertEquals("http://localhost:54321", SupabaseConfigManager.formatUrl("http://localhost:54321/"))
    }

    @Test
    fun testSupabaseConfigManager_placeholderDetection() {
        assertTrue(SupabaseConfigManager.isPlaceholderUrl("https://your-project-ref.supabase.co"))
        assertTrue(SupabaseConfigManager.isPlaceholderUrl("https://chating-free-storage.supabase.co"))
        assertTrue(SupabaseConfigManager.isPlaceholderUrl(""))
        assertFalse(SupabaseConfigManager.isPlaceholderUrl("https://myrealproject123.supabase.co"))

        assertTrue(SupabaseConfigManager.isPlaceholderKey("your-supabase-anon-key"))
        assertTrue(SupabaseConfigManager.isPlaceholderKey(""))
        assertFalse(SupabaseConfigManager.isPlaceholderKey("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.realkey"))
    }

    @Test
    fun testSupabaseStorageService_publicUrlGeneration() {
        val publicUrl = SupabaseStorageService.getPublicUrl("profile-photos", "user123/avatar.jpg")
        assertTrue(publicUrl.contains("/storage/v1/object/public/profile-photos/user123/avatar.jpg"))
    }
}
