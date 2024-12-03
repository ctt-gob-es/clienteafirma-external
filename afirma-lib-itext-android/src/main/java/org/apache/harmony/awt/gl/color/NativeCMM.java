/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Oleg V. Khaschansky
 */
package org.apache.harmony.awt.gl.color;

import java.util.HashMap;
import java.util.LinkedHashMap;

import harmony.java.awt.color.ICC_Profile;

/**
 * This class is a wrapper for the native CMM library
 */
public class NativeCMM {

	/**
	 * Storage for profile handles, since they are private in ICC_Profile, but
	 * we need access to them.
	 */
	private static HashMap<ICC_Profile, Long> profileHandles = new LinkedHashMap<ICC_Profile, Long>();

	private static boolean isCMMLoaded;

	public static void addHandle(final ICC_Profile key, final long handle) {
		profileHandles.put(key, new Long(handle));
	}

	public static void removeHandle(final ICC_Profile key) {
		profileHandles.remove(key);
	}

	public static long getHandle(final ICC_Profile key) {
		return profileHandles.get(key).longValue();
	}

	/* ICC profile management */
	public static long cmmOpenProfile(final byte[] data) {
		// No hacemos nada
		return 1;
	}

	public static void cmmCloseProfile(final long profileID) {
		// No hacemos nada
	}

	public static int cmmGetProfileSize(final long profileID) {
		// No hacemos nada
		return 1;
	}

	public static void cmmGetProfile(final long profileID, final byte[] data) {
		// No hacemos nada
	}

	public static int cmmGetProfileElementSize(final long profileID, final int signature) {
		// No hacemos nada
		return 1;
	}

	public static void cmmGetProfileElement(final long profileID, final int signature, final byte[] data) {
		// No hacemos nada
	}

	public static void cmmSetProfileElement(final long profileID, final int tagSignature, final byte[] data) {
		// No hacemos nada
	}

	/* ICC transforms */
	public static long cmmCreateMultiprofileTransform(final long[] profileHandles, final int[] renderingIntents) {
		// No hacemos nada
		return 1;
	}

	public static void cmmDeleteTransform(final long transformHandle) {
		// No hacemos nada
	}

	public static void cmmTranslateColors(final long transformHandle, final NativeImageFormat src, final NativeImageFormat dest) {
		// No hacemos nada
	}

	static void loadCMM() {
		// if (!isCMMLoaded) {
		// AccessController.doPrivileged(
		// new PrivilegedAction<Void>() {
		// public Void run() {
		//                        org.apache.harmony.awt.Utils.loadLibrary("lcmm"); //$NON-NLS-1$
		// return null;
		// }
		// } );
		// isCMMLoaded = true;
		// }
	}

	/* load native CMM library */
	static {
		loadCMM();
	}
}
