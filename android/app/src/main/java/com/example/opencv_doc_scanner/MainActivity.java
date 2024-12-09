package  com.example.opencv_doc_scanner;
import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import android.content.Intent;
import android.provider.MediaStore;




public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.example.opencv_doc_scanner";

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
            .setMethodCallHandler(
                (MethodCall call, MethodChannel.Result result) -> {
                    if (call.method.equals("printHelloWorld")) {
                        String message = printHelloWorld();
                        result.success(message); // Return message to Flutter
                    } else  if (call.method.equals("openOpenCvCamera")) {
                        Intent intent = new Intent(MainActivity.this, OpenCvCameraActivity.class);
                        startActivity(intent);
                        result.success("OpenCV Camera Launched");
                    } else if(call.method.equals("openCamera")){
                        openCamera();
                        result.success("Camera opened");
                    }
                   
                    
                    else {
                        result.notImplemented();
                    }
                }
            );
    }

    private String printHelloWorld() {
        System.out.println("Hello, World! - from Java");
        return "Hello from Java!";
    }




    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(cameraIntent);
        }
    }

}