#import <jni.h>
#import <jawt.h>
#import <jawt_md.h>
#import <Cocoa/Cocoa.h>
#import "org_jezve_os_MinSize.h"

// Given a Java component, return an NSWindow*
NSWindow * GetWindowFromComponent(jobject parent, JNIEnv *env) {
    JAWT awt;
    JAWT_DrawingSurface* ds;
    JAWT_DrawingSurfaceInfo* dsi;
    JAWT_MacOSXDrawingSurfaceInfo* dsi_mac;
    jboolean result;
    jint lock;

    // Get the AWT
    awt.version = JAWT_VERSION_1_4;
    result = JAWT_GetAWT(env, &awt);
    assert(result != JNI_FALSE);

    // Get the drawing surface
    ds = awt.GetDrawingSurface(env, parent);
    assert(ds != NULL);

    // Lock the drawing surface
    lock = ds->Lock(ds);
    assert((lock & JAWT_LOCK_ERROR) == 0);

    // Get the drawing surface info
    dsi = ds->GetDrawingSurfaceInfo(ds);

    // Get the platform-specific drawing info
    dsi_mac = (JAWT_MacOSXDrawingSurfaceInfo*)dsi->platformInfo;

    // Get the NSView corresponding to the component that was passed
    NSView *view = dsi_mac->cocoaViewRef;
  
    // Free the drawing surface info
    ds->FreeDrawingSurfaceInfo(dsi);
    // Unlock the drawing surface
    ds->Unlock(ds);

    // Free the drawing surface
    awt.FreeDrawingSurface(ds);
  
    // Get the view's parent window; this is what we need to show a sheet
    return [view window];
}

/*
 * Class:     org_jezve_os_MinSize
 * Method:    setContentMinSize
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_jezve_os_MinSize_setContentMinSize(JNIEnv *env, jobject c, jint w, jint h) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    // Take the parent component (passed via Java call) and get the parent NSWindow from it
    NSWindow *window = GetWindowFromComponent(c, env);
    if (window != NULL) {
        NSSize size;
        size.width = w;
        size.height = h;
        [window setContentMinSize:size];
    }
    [pool release];
}

