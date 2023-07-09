# android_app_camera2_streaming_server
Android Camera Streamer Demo Wiki Page
Contact:
Research activity done under the guidance of 
Professor Kaikai Liu,
Dept of Computer Engineering,
San Jose State University
RA: Unnikrishnan Sreekumar (011445889)

Aim:  
To design and implement:  
A Server android app which would:  
a) Capture camera images  
b) encode the video at 25 frames per second using H.264 (could use MediaCodec android API to use the hardware codec)  
c) RTP packetize the video and  
d) stream over to a client (UDP)  

A Client app, linux based:  
a) accept the incoming data stream  
b) RTP de-packetize and isolate individual frames  
c) decode the frames  
d) display the video frames on a local window



Android API took a shift from the exposed android.hardware.Camera class to the complete pipeline based Camera control API at android.hardware.camera2.  
There were not much material online, so here I shall log a brief Wiki on the camera2 API usage in-order to build an application that shall show a camera preview and simultaneously encode the camera raw buffers.

### Camera2 Preview Capture:  
A SurfaceTexture on which you can draw the camera preview.   
Thus the layout for camera activity shall have:  
```xml
<TextureView
   android:id="@+id/textureCamera"
   android:layout_width="wrap_content"
   android:layout_height="wrap_content"
   android:layout_alignParentLeft="true"
   android:layout_alignParentTop="true"/>
```
Now, we could create a TextureView object from the above layout on which the added SurfaceTextureListener let you know when the surface is created and configured.  
Once the TextureView is configured, we could open the main Camera using:   
CameraManager.openCamera() API.  
Once the camera is opened, at CameraDevice.StateCallback.onOpened(), all we have to do is create a new CaptureRequest using the opened CameraDevice.createCaptureRequest for TEMPLATE_PREVIEW.  
Now, request a new CameraCaptureSession using the opened CameraDevice.createCaptureSession() API.  
Once the camera capture session is up, at CameraCaptureSession.StateCallback.onConfigured(), just feed in the created CaptureRequest.Builder for a repeated image capture and dump mode using the CameraCaptureSession.setRepeatingRequest() API.  
The above description with in conjunction with the code uploaded at https://github.com/unnikrishnankgs/android_app_camera2_streaming_server, you’d be able to start see the preview on the created SurfaceTexture on your Camera Activity.  


### Camera2 video encode:  
We could use MediaCodec API which provide hardware codec access to:  
a) Create a AVC/H.264 encoder suing MediaCodec.createEncoderByType(“video/avc”) API  
b) configure the codec for the required framerate, bitrate and such  
c) Get the input Surface object using the codec.createInputSurface()  
With the above input surface to the encoder created, now thanks to the CameraCaptureSession we created above, We just have to now slightly modify the code above by passing this new Surface as well with the Surface around SurfaceTexture to the CameraDevice.createCameraCaptureSession() API.  
Now, that we connected the camera to the encoder, we could just get the encoder output asynchronously using the blocking API: MediaCodec.dequeueOutputBuffer() in conjunction with MediaCodec.getOutputBuffer() API to get the encoded buffer in java.nio.ByteBuffer objects.  
The above ByteBuffer objects could now be RTP packetized for our demo.  

### RTP Packetization:  
Real Time Transport Protocol is a transport protocol in the application layer TCP/IP model for real-time applications. It’s detailed under http://www.rfc-base.org/txt/rfc-3550.txt with a 12 byte header format.

For this demo, we are using a custom header 5 bytes long where first byte indicate if the current packet is a video-frame start or continue frame. When it’s the starting packet, 4 bytes following this will have the total length of the video-frame.

We will have to implement the full RTP packetizer to transport all the frame information including timestamps, codec info, picture info and such. We might have to consider other encapsulation protocols like MPEG2-TS if the application require audio multiplexing and synchronisation.

## Data Streaming:


### Sender:  
In this demo application, we support data streaming on UDP (User Datagram Protocol) as well as TCP (Transmission Control Protocol). The network between the two devices is now a normal TCP/IP link over LAN. For further improvement in the latency and throughput, we could use WiFi-Direct to connect the two devices rather than routing the packets via a central router/access-point.  

### Receiver:  
The data receiver is a simple command line application written in C with dedicated threads to accept incoming connections and receive data over UDP/TCP and another thread to process the received data.

## Data Processing:  
We use the GStreamer multimedia framework to process the depacketized video frames. GStreamer’s appsrc plugin provide us a mechanism to feed in to our custom pipeline, the video frames.   
Our current pipeline is:  
`Sream-source ---> H.264 Decoder → GL Image Sink.`  
GStreamer: https://gstreamer.freedesktop.org/  
GStreamer installation and source code build wiki: https://gstreamer.freedesktop.org/documentation/frequently-asked-questions/getting.html  

## Code:

Sender/Server code (Android App):  
https://github.com/unnikrishnankgs/android_app_camera2_streaming_server


Receiver/Client code (Linux server; Server implementation in C):  
https://github.com/unnikrishnankgs/basic-stream-receiver

To study just the camera2 API:  
https://github.com/unnikrishnankgs/android-camera2-streaming

