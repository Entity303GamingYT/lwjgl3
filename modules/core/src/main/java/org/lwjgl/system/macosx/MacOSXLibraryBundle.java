/*
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
package org.lwjgl.system.macosx;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.SharedLibrary;

import java.nio.ByteBuffer;

import static org.lwjgl.system.Checks.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.macosx.CoreFoundation.*;

/** Implements a {@link SharedLibrary} on the MacOS X using {@code CFBundle}. */
public class MacOSXLibraryBundle extends MacOSXLibrary {

	public MacOSXLibraryBundle(String name, long bundleRef) {
		super(bundleRef, name);
	}

	public static MacOSXLibraryBundle getWithIdentifier(String bundleID) {
		long filePath = NULL;
		try ( MemoryStack stack = stackPush() ) {
			filePath = CString2CFString(stack.ASCII(bundleID), kCFStringEncodingASCII);

			long bundleRef = CFBundleGetBundleWithIdentifier(filePath);
			if ( bundleRef == NULL )
				throw new UnsatisfiedLinkError("Failed to retrieve bundle with identifier: " + bundleID);

			CFRetain(bundleRef);
			return new MacOSXLibraryBundle(bundleID, bundleRef);
		} finally {
			if ( filePath != NULL ) CFRelease(filePath);
		}
	}

	public static MacOSXLibraryBundle create(String path) {
		long filePath = NULL;
		long url = NULL;
		try ( MemoryStack stack = stackPush() ) {
			filePath = CString2CFString(stack.UTF8(path), kCFStringEncodingUTF8);
			url = checkPointer(CFURLCreateWithFileSystemPath(NULL, filePath, kCFURLPOSIXPathStyle, true));

			long bundleRef = CFBundleCreate(NULL, url);
			if ( bundleRef == NULL )
				throw new UnsatisfiedLinkError("Failed to create bundle: " + path);

			return new MacOSXLibraryBundle(path, bundleRef);
		} finally {
			if ( url != NULL ) CFRelease(url);
			if ( filePath != NULL ) CFRelease(filePath);
		}
	}

	@Override
	public long getFunctionAddress(ByteBuffer functionName) {
		long nameRef = CString2CFString(functionName, kCFStringEncodingASCII);
		try {
			return CFBundleGetFunctionPointerForName(address(), nameRef);
		} finally {
			CFRelease(nameRef);
		}
	}

	private static long CString2CFString(ByteBuffer name, int encoding) {
		return checkPointer(CFStringCreateWithCStringNoCopy(NULL, name, encoding, kCFAllocatorNull));
	}

	@Override
	public void free() {
		CFRelease(address());
	}

}