package sjsu.com.camera2demo;

/**
 * Created by gotham on 11/04/17.
 */
        import android.app.Fragment;
        import android.os.Bundle;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.app.Activity;
import android.util.Log;

public class Camera2RawFragment extends Fragment {
    private AutoFitTextureView mTextureView;
    static private MainActivity mParentActivity;

    public static Camera2RawFragment newInstance(Activity a) {
        mParentActivity = (MainActivity) a;
        return new Camera2RawFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v("Camera2RawFragment", "onCreateView");
        return inflater.inflate(R.layout.fragment_camera2, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        Log.v("Camera2RawFragment", "onViewCreated");
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.textureCamera);
        mParentActivity.openSurfaceTexture(mTextureView);
    }

}