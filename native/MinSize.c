#define STRICT 1                                                                                                                                                                                                 
#define LEAN_AND_MEAN 1
#include <jawt_md.h>

WNDPROC g_oldWndProc;
LONG g_minWidth, g_minHeight;

LRESULT CALLBACK MinSizeWindowProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
    if (uMsg == WM_DESTROY)
        SetWindowLong(hwnd, GWL_WNDPROC, (LONG)g_oldWndProc);
    LRESULT result = CallWindowProc(g_oldWndProc, hwnd, uMsg, wParam, lParam);
    switch(uMsg) {
    case WM_GETMINMAXINFO:
        {
            MINMAXINFO *pmmi = (MINMAXINFO *)lParam;
            RECT rc = {0, 0, g_minWidth, g_minHeight};
            AdjustWindowRectEx(&rc, GetWindowLong(hwnd, GWL_STYLE), TRUE, GetWindowLong(hwnd, GWL_EXSTYLE));
            pmmi->ptMinTrackSize.x = rc.right - rc.left;
            pmmi->ptMinTrackSize.y = rc.bottom - rc.top;
            return 0;
        }
        break;
    }
    return result;
}

// Given a Java component, return an HWND
HWND GetWindowFromComponent(jobject parent, JNIEnv *env) {
    JAWT awt;
    JAWT_DrawingSurface* ds;
    JAWT_DrawingSurfaceInfo* dsi;
    JAWT_Win32DrawingSurfaceInfo *dsi_win;
    jboolean result;
    jint lock;
    HWND hwnd;

    // Get the AWT
    awt.version = JAWT_VERSION_1_4;
    result = JAWT_GetAWT(env, &awt);

    // Get the drawing surface
    ds = awt.GetDrawingSurface(env, parent);

    // Lock the drawing surface
    lock = ds->Lock(ds);

    // Get the drawing surface info
    dsi = ds->GetDrawingSurfaceInfo(ds);

    // Get the platform-specific drawing info
    dsi_win = (JAWT_Win32DrawingSurfaceInfo *)dsi->platformInfo;

    hwnd = dsi_win->hwnd;    
  
    // Free the drawing surface info
    ds->FreeDrawingSurfaceInfo(dsi);
    // Unlock the drawing surface
    ds->Unlock(ds);

    // Free the drawing surface
    awt.FreeDrawingSurface(ds);
  
    // Get the view's parent window; this is what we need to show a sheet
    return hwnd;
}

/*
 * Class:     org_jezve_os_MinSize
 * Method:    setContentMinSize
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_jezve_os_MinSize_setContentMinSize(JNIEnv *env, jobject c, jint w, jint h) {
    HWND hwnd = GetWindowFromComponent(c, env);
    g_minWidth = w;
    g_minHeight = h;
    g_oldWndProc = (WNDPROC)SetWindowLong((HWND)hwnd, GWL_WNDPROC, (LONG)MinSizeWindowProc);
}
