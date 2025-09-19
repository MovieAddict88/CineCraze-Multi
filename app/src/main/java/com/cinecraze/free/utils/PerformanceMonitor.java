package com.cinecraze.free.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

import java.util.List;

/**
 * Performance monitoring utility for tracking memory usage and performance metrics
 * Helps identify bottlenecks when handling large datasets
 */
public class PerformanceMonitor {

    private static final String TAG = "PerformanceMonitor";

    /**
     * Get current memory usage in MB
     */
    public static long getCurrentMemoryUsage() {
        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);
        return memoryInfo.getTotalPss() / 1024; // Convert to MB
    }

    /**
     * Get available memory in MB
     */
    public static long getAvailableMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.availMem / (1024 * 1024); // Convert to MB
    }

    /**
     * Get total memory in MB
     */
    public static long getTotalMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.totalMem / (1024 * 1024); // Convert to MB
    }

    /**
     * Check if device is low on memory
     */
    public static boolean isLowMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo.lowMemory;
    }

    /**
     * Log memory usage with context
     */
    public static void logMemoryUsage(String context) {
        long currentMemory = getCurrentMemoryUsage();
        Log.d(TAG, context + " - Current Memory Usage: " + currentMemory + " MB");
    }

    /**
     * Log memory usage with context and additional info
     */
    public static void logMemoryUsage(Context androidContext, String context) {
        long currentMemory = getCurrentMemoryUsage();
        long availableMemory = getAvailableMemory(androidContext);
        long totalMemory = getTotalMemory(androidContext);
        boolean lowMemory = isLowMemory(androidContext);

        Log.d(TAG, context + " - Memory: " + currentMemory + "MB used, " +
              availableMemory + "MB available, " + totalMemory + "MB total, Low: " + lowMemory);
    }

    /**
     * Monitor memory usage during data processing
     */
    public static class MemoryTracker {
        private long startMemory;
        private String operation;

        public MemoryTracker(String operation) {
            this.operation = operation;
            this.startMemory = getCurrentMemoryUsage();
            Log.d(TAG, "Starting " + operation + " - Initial Memory: " + startMemory + " MB");
        }

        public void checkpoint(String checkpoint) {
            long currentMemory = getCurrentMemoryUsage();
            long delta = currentMemory - startMemory;
            Log.d(TAG, operation + " - " + checkpoint + " - Memory: " + currentMemory + " MB (Δ" + delta + " MB)");
        }

        public void finish() {
            long endMemory = getCurrentMemoryUsage();
            long totalDelta = endMemory - startMemory;
            Log.d(TAG, "Finished " + operation + " - Final Memory: " + endMemory + " MB (Total Δ" + totalDelta + " MB)");
        }
    }

    /**
     * Performance timer for measuring operation duration
     */
    public static class PerformanceTimer {
        private long startTime;
        private String operation;

        public PerformanceTimer(String operation) {
            this.operation = operation;
            this.startTime = System.currentTimeMillis();
            Log.d(TAG, "Starting " + operation);
        }

        public void checkpoint(String checkpoint) {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - startTime;
            Log.d(TAG, operation + " - " + checkpoint + " - Elapsed: " + elapsed + " ms");
        }

        public long finish() {
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            Log.d(TAG, "Finished " + operation + " - Total Time: " + totalTime + " ms");
            return totalTime;
        }
    }

    /**
     * Get memory usage statistics as a formatted string
     */
    public static String getMemoryStats(Context context) {
        long currentMemory = getCurrentMemoryUsage();
        long availableMemory = getAvailableMemory(context);
        long totalMemory = getTotalMemory(context);
        boolean lowMemory = isLowMemory(context);

        return String.format("Memory: %dMB used, %dMB available, %dMB total, Low: %s",
                           currentMemory, availableMemory, totalMemory, lowMemory);
    }

    /**
     * Check if we should reduce memory usage based on available memory
     */
    public static boolean shouldReduceMemoryUsage(Context context) {
        long availableMemory = getAvailableMemory(context);
        long totalMemory = getTotalMemory(context);
        boolean lowMemory = isLowMemory(context);

        // Reduce memory usage if:
        // 1. Low memory flag is set
        // 2. Available memory is less than 20% of total memory
        return lowMemory || (availableMemory < (totalMemory * 0.2));
    }

    /**
     * Get recommended page size based on available memory
     */
    public static int getRecommendedPageSize(Context context) {
        if (shouldReduceMemoryUsage(context)) {
            return 10; // Smaller page size for low memory devices
        } else {
            long availableMemory = getAvailableMemory(context);
            if (availableMemory > 1000) { // More than 1GB available
                return 50; // Larger page size for high memory devices
            } else if (availableMemory > 500) { // More than 500MB available
                return 30; // Medium page size
            } else {
                return 20; // Default page size
            }
        }
    }
}