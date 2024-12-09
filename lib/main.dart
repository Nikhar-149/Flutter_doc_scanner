import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  static const platform = MethodChannel('com.example.opencv_doc_scanner');

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: Text('Flutter to Native Example')),
        body: Column(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            Center(
                child: ElevatedButton(
                    onPressed: () async {
                      try {
                        await platform.invokeMethod('openOpenCvCamera');
                      } catch (e) {
                        print('Error: $e');
                      }
                    },
                    child: Text('Open OpenCv Camera'))),
            Center(
                child: ElevatedButton(
                    onPressed: () async {
                      try {
                        await platform.invokeMethod('openCamera');
                      } catch (e) {
                        print('Error: $e');
                      }
                    },
                    child: Text('Open Camera'))),
            Center(
              child: ElevatedButton(
                onPressed: () async {
                  try {
                    final String result =
                        await platform.invokeMethod('printHelloWorld');
                    print(result); // Should print the response from Java
                  } catch (e) {
                    print('Error: $e');
                  }
                },
                child: Text('Call Java Native Code'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
