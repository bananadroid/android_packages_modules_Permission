/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.permissioncontroller.permission.utils

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Looper
import android.os.UserHandle
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * Gets an [Application] instance from a regular [Context]
 */
val Context.application: Application get() = when (this) {
    is Application -> this
    is Activity -> application
    is Service -> application
    is ContextWrapper -> baseContext.application
    else -> applicationContext as Application
}

/**
 * The number of threads in the IPC thread pool. If too many threads are used, transactions may
 * fail. For instance, if a user has several child profiles, this can lead to several threads
 * simultaneously fetching the installed packages of each, which exhausts binder buffer space.
 * To avoid this, we deliberately choose a conservative thread count.
 */
const val IPC_THREAD_POOL_COUNT = 4

/**
 * A coroutine dispatcher with a fixed thread pool size, to be used for background tasks
 */
val IPC = Executors.newFixedThreadPool(IPC_THREAD_POOL_COUNT).asCoroutineDispatcher()

/**
 * Assert that an operation is running on main thread
 */
fun ensureMainThread() = check(Looper.myLooper() == Looper.getMainLooper()) {
    "Only meant to be used on the main thread"
}

/**
 * A more readable version of [PackageManager.updatePermissionFlags]
 */
fun PackageManager.updatePermissionFlags(
    permissionName: String,
    packageName: String,
    user: UserHandle,
    vararg flags: Pair<Int, Boolean>
) {
    val mask = flags.fold(0, { mask, (flag, _) -> mask or flag })
    val value = flags.fold(0,
        { mask2, (flag, flagValue) -> if (flagValue) mask2 or flag else mask2 })
    updatePermissionFlags(permissionName, packageName, mask, value, user)
}

/**
 * Gets a [ComponentInfo] from a [ResolveInfo]
 */
val ResolveInfo.componentInfo: ComponentInfo
    get() {
        return (activityInfo as ComponentInfo?)
                ?: serviceInfo
                ?: providerInfo
                ?: throw IllegalStateException("Missing ComponentInfo!")
    }