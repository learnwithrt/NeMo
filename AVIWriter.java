/**
 * @(#)AVIWriter.java  
 *
 * Copyright (c) 2011 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance onlyWith the
 * license agreement you entered into onlyWith Werner Randelshofer.
 * For details see accompanying license terms.
 */

import java.util.EnumSet;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.*;
import java.nio.ByteOrder;
import java.util.Arrays;
import javax.imageio.stream.*;
/**
 * Provides high-level support for encoding and writing audio and video samples
 * into an AVI 1.0 file.
 *
 * @author Werner Randelshofer
 * @version $Id: AVIWriter.java 192 2012-03-29 22:00:37Z werner $
 */
public class AVIWriter extends AVIOutputStream implements MovieWriter {

    public final static Format AVI = new Format(VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.FILE, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_AVI);
    public final static Format VIDEO_RAW = new Format(
            FormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, FormatKeys.MimeTypeKey, FormatKeys.MIME_AVI,
            FormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_DIB, VideoFormatKeys.CompressorNameKey, VideoFormatKeys.COMPRESSOR_NAME_QUICKTIME_RAW);
    public final static Format VIDEO_JPEG = new Format(
            VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_AVI,
            VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_MJPG, VideoFormatKeys.CompressorNameKey, VideoFormatKeys.COMPRESSOR_NAME_QUICKTIME_RAW);
    public final static Format VIDEO_PNG = new Format(
            VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_AVI,
            VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_PNG, VideoFormatKeys.CompressorNameKey, VideoFormatKeys.COMPRESSOR_NAME_QUICKTIME_RAW);
    public final static Format VIDEO_RLE = new Format(
            VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_AVI,
            VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_RLE, VideoFormatKeys.CompressorNameKey, VideoFormatKeys.COMPRESSOR_NAME_QUICKTIME_RAW);
    public final static Format VIDEO_SCREEN_CAPTURE = new Format(
            VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_AVI,
            VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE, VideoFormatKeys.CompressorNameKey, VideoFormatKeys.COMPRESSOR_NAME_QUICKTIME_RAW);

    /**
     * Creates a new AVI writer.
     *
     * @param file the output file
     */
    public AVIWriter(File file) throws IOException {
        super(file);
    }

    /**
     * Creates a new AVI writer.
     *
     * @param out the output stream.
     */
    public AVIWriter(ImageOutputStream out) throws IOException {
        super(out);
    }

    @Override
    public Format getFileFormat() throws IOException {
        return AVI;
    }

    @Override
    public Format getFormat(int track) {
        return tracks.get(track).format;
    }
    /** Returns the media duration of the track in seconds. */
    @Override
    public Rational getDuration(int track) {
        Track tr=tracks.get(track);
        long duration=getMediaDuration(track);
        return new Rational(duration*tr.scale,tr.rate);
    }

    /** Adds a track.
     *
     * @param format The format of the track.
     * @return The track number.
     */
    @Override
    public int addTrack(Format format) throws IOException {
        if (format.get(VideoFormatKeys.MediaTypeKey) == FormatKeys.MediaType.VIDEO) {
            return addVideoTrack(format);
        } else {
            return addAudioTrack(format);
        }
    }

    /** Adds a video track.
     *
     * @param format The format of the track.
     * @return The track number.
     */
    private int addVideoTrack(Format vf) throws IOException {
        if (!vf.containsKey(VideoFormatKeys.EncodingKey)) {
            throw new IllegalArgumentException("EncodingKey missing in "+vf);
        }
        if (!vf.containsKey(VideoFormatKeys.FrameRateKey)) {
            throw new IllegalArgumentException("FrameRateKey missing in "+vf);
        }
        if (!vf.containsKey(VideoFormatKeys.WidthKey)) {
            throw new IllegalArgumentException("WidthKey missing in "+vf);
        }
        if (!vf.containsKey(VideoFormatKeys.HeightKey)) {
            throw new IllegalArgumentException("HeightKey missing in "+vf);
        }
        if (!vf.containsKey(VideoFormatKeys.DepthKey)) {
            throw new IllegalArgumentException("DepthKey missing in "+vf);
        }
        return addVideoTrack(vf.get(VideoFormatKeys.EncodingKey),
                vf.get(VideoFormatKeys.FrameRateKey).getDenominator(), vf.get(VideoFormatKeys.FrameRateKey).getNumerator(),
                vf.get(VideoFormatKeys.WidthKey), vf.get(VideoFormatKeys.HeightKey), vf.get(VideoFormatKeys.DepthKey),
                vf.get(VideoFormatKeys.FrameRateKey).floor(1).intValue());
    }

    /** Adds an audio track.
     *
     * @param format The format of the track.
     * @return The track number.
     */
    private int addAudioTrack(Format format) throws IOException {
        int waveFormatTag = 0x0001; // WAVE_FORMAT_PCM


        long timeScale = 1;
        long sampleRate = format.get(AudioFormatKeys.SampleRateKey, new Rational(41000, 0)).longValue();
        int numberOfChannels = format.get(AudioFormatKeys.ChannelsKey, 1);
        int sampleSizeInBits = format.get(AudioFormatKeys.SampleSizeInBitsKey, 16); //
        boolean isCompressed = false; // FIXME
        int frameDuration = 1;
        int frameSize = format.get(AudioFormatKeys.FrameSizeKey, (sampleSizeInBits + 7) / 8 * numberOfChannels);


        String enc = format.get(AudioFormatKeys.EncodingKey);
        if (enc == null) {
            waveFormatTag = 0x0001; // WAVE_FORMAT_PCM
        } else if (enc.equals(AudioFormatKeys.ENCODING_ALAW)) {
            waveFormatTag = 0x0001; // WAVE_FORMAT_PCM
        } else if (enc.equals(AudioFormatKeys.ENCODING_PCM_SIGNED)) {
            waveFormatTag = 0x0001; // WAVE_FORMAT_PCM
        } else if (enc.equals(AudioFormatKeys.ENCODING_PCM_UNSIGNED)) {
            waveFormatTag = 0x0001; // WAVE_FORMAT_PCM
        } else if (enc.equals(AudioFormatKeys.ENCODING_ULAW)) {
            waveFormatTag = 0x0001; // WAVE_FORMAT_PCM
        } else if (enc.equals(AudioFormatKeys.ENCODING_MP3)) {
            waveFormatTag = 0x0001; // WAVE_FORMAT_PCM - FIXME
        } else {
            waveFormatTag = RIFFParser.stringToID(format.get(AudioFormatKeys.EncodingKey)) & 0xffff;
        }

        return addAudioTrack(waveFormatTag, //
                timeScale, sampleRate, //
                numberOfChannels, sampleSizeInBits, //
                isCompressed, //
                frameDuration, frameSize);
    }

    /** Returns the codec of the specified track. */
    public Codec getCodec(int track) {
        return tracks.get(track).codec;
    }

    /** Sets the codec for the specified track. */
    public void setCodec(int track, Codec codec) {
        tracks.get(track).codec = codec;
    }

    @Override
    public int getTrackCount() {
        return tracks.size();
    }

    /**
     * Encodes the provided image and writes its sample data into the specified track.
     *
     * @param track The track index.
     * @param image The image of the video frame.
     * @param duration Duration given in media time units.
     *
     * @throws IndexOutofBoundsException if the track index is out of bounds.
     * @throws if the duration is less than 1, or if the dimension of the frame
     * does not match the dimension of the video.
     * @throws UnsupportedOperationException if the {@code MovieWriter} does not have
     * a built-in encoder for this video format.
     * @throws IOException if writing the sample data failed.
     */
    public void write(int track, BufferedImage image, long duration) throws IOException {
        ensureStarted();

        VideoTrack vt = (VideoTrack) tracks.get(track);
        if (vt.codec == null) {
            createCodec(vt);
        }
        if (vt.codec == null) {
            throw new UnsupportedOperationException("No codec for this format: "+vt.format);
        }

        // The dimension of the image must match the dimension of the video track
        Format fmt = vt.format;
        if (fmt.get(VideoFormatKeys.WidthKey) != image.getWidth() || fmt.get(VideoFormatKeys.HeightKey) != image.getHeight()) {
            throw new IllegalArgumentException("Dimensions of image[" + vt.samples.size()
                    + "] (width=" + image.getWidth() + ", height=" + image.getHeight()
                    + ") differs from video format of track: " + fmt);
        }

        // Encode pixel data
        {
            if (vt.outputBuffer == null) {
                vt.outputBuffer = new Buffer();
            }

            boolean isKeyframe = vt.syncInterval == 0 ? false : vt.samples.size() % vt.syncInterval == 0;

            Buffer inputBuffer = new Buffer();
            inputBuffer.flags = (isKeyframe) ? EnumSet.of(BufferFlag.KEYFRAME) : EnumSet.noneOf(BufferFlag.class);
            inputBuffer.data = image;
            vt.codec.process(inputBuffer, vt.outputBuffer);
            if (vt.outputBuffer.flags.contains(BufferFlag.DISCARD)) {
                return;
            }

            // Encode palette data
            isKeyframe = vt.outputBuffer.flags.contains(BufferFlag.KEYFRAME);
            boolean paletteChange = writePalette(track, image, isKeyframe);
            writeSample(track, (byte[])vt.outputBuffer.data,vt.outputBuffer.offset,vt.outputBuffer.length, isKeyframe&&!paletteChange);
/*
            long offset = getRelativeStreamPosition();

            DataChunk videoFrameChunk = new DataChunk(vt.getSampleChunkFourCC(isKeyframe));
            moviChunk.add(videoFrameChunk);
            videoFrameChunk.getOutputStream().write((byte[]) vt.outputBuffer.data, vt.outputBuffer.offset, vt.outputBuffer.length);
            videoFrameChunk.finish();
            long length = getRelativeStreamPosition() - offset;

            Sample s=new Sample(videoFrameChunk.chunkType, 1, offset, length, isKeyframe&&!paletteChange);
            vt.addSample(s);
            idx1.add(s);
            
            if (getRelativeStreamPosition() > 1L << 32) {
                throw new IOException("AVI file is larger than 4 GB");
            }*/
        }
    }

    /** Encodes the data provided in the buffer and then writes it into
     * the specified track.
     * <p>
     * Does nothing if the discard-flag in the buffer is set to true.
     *
     * @param track The track number.
     * @param buf The buffer containing a data sample.
     */
    @Override
    public void write(int track, Buffer buf) throws IOException {
        ensureStarted();
        if (buf.flags.contains(BufferFlag.DISCARD)) {
            return;
        }
        
        Track tr = tracks.get(track);

        boolean isKeyframe = buf.flags.contains(BufferFlag.KEYFRAME);
        if (buf.data instanceof BufferedImage) {
            if (tr.syncInterval != 0) {
                isKeyframe = buf.flags.contains(BufferFlag.KEYFRAME) | (tr.samples.size() % tr.syncInterval == 0);
            }
        }
        // Encode palette data
        boolean paletteChange = false;
        if (buf.data instanceof BufferedImage && tr instanceof VideoTrack) {
            paletteChange = writePalette(track, (BufferedImage) buf.data, isKeyframe);
        } else if (buf.header instanceof IndexColorModel) {
            paletteChange = writePalette(track, (IndexColorModel) buf.header, isKeyframe);
        }
        // Encode sample data
        {
            if (buf.format.removeKeys(VideoFormatKeys.FrameRateKey).matches(tr.format) && buf.data instanceof byte[]) {
                writeSamples(track, buf.sampleCount, (byte[]) buf.data, buf.offset, buf.length,
                        buf.isFlag(BufferFlag.KEYFRAME) && !paletteChange);
                return;
            }
            
            // We got here, because the buffer format does not match the track 
            // format. Lets see if we can create a codec which can perform the
            // encoding for us.

            if (tr.codec == null) {
                createCodec(tr);
                if (tr.codec == null) {
                    throw new UnsupportedOperationException("No codec for this format " + tr.format + "(" + typeToInt(tr.format.get(FormatKeys.EncodingKey)) + ")");
                }
            }

            if (tr.outputBuffer == null) {
                tr.outputBuffer = new Buffer();
            }
            Buffer outBuf = tr.outputBuffer;
            if (tr.codec.process(buf, outBuf) != Codec.CODEC_OK) {
                throw new IOException("Codec failed or could not encode the sample in a single step.");
            }
            if (outBuf.isFlag(BufferFlag.DISCARD)) {
                return;
            }
            writeSamples(track, outBuf.sampleCount, (byte[]) outBuf.data, outBuf.offset, outBuf.length,
                    isKeyframe&&!paletteChange);
        }
    }

    private boolean writePalette(int track, BufferedImage image, boolean isKeyframe) throws IOException {
        if ((image.getColorModel() instanceof IndexColorModel)) {
            return writePalette(track, (IndexColorModel) image.getColorModel(), isKeyframe);
        }
        return false;
    }

    private boolean writePalette(int track, IndexColorModel imgPalette, boolean isKeyframe) throws IOException {
        ensureStarted();

        VideoTrack vt = (VideoTrack) tracks.get(track);
        int imgDepth = vt.bitCount;
        ByteArrayImageOutputStream tmp = null;
        boolean paletteChange=false;
        switch (imgDepth) {
            case 4: {
                //IndexColorModel imgPalette = (IndexColorModel) image.getColorModel();
                int[] imgRGBs = new int[16];
                imgPalette.getRGBs(imgRGBs);
                int[] previousRGBs = new int[16];
                if (vt.previousPalette == null) {
                    vt.previousPalette = vt.palette;
                }
                vt.previousPalette.getRGBs(previousRGBs);
                if (isKeyframe || !Arrays.equals(imgRGBs, previousRGBs)) {
                    paletteChange=true;
                    vt.previousPalette = imgPalette;
                    /*
                    int first = imgPalette.getMapSize();
                    int last = -1;
                    for (int i = 0; i < 16; i++) {
                    if (previousRGBs[i] != imgRGBs[i] && i < first) {
                    first = i;
                    }
                    if (previousRGBs[i] != imgRGBs[i] && i > last) {
                    last = i;
                    }
                    }*/
                    int first = 0;
                    int last = imgPalette.getMapSize() - 1;
                    /*
                     * typedef struct {
                    BYTE         bFirstEntry;
                    BYTE         bNumEntries;
                    WORD         wFlags;
                    PALETTEENTRY peNew[];
                    } AVIPALCHANGE;
                     *
                     * typedef struct tagPALETTEENTRY {
                    BYTE peRed;
                    BYTE peGreen;
                    BYTE peBlue;
                    BYTE peFlags;
                    } PALETTEENTRY;
                     */
                    tmp = new ByteArrayImageOutputStream(ByteOrder.LITTLE_ENDIAN);
                    tmp.writeByte(first);//bFirstEntry
                    tmp.writeByte(last - first + 1);//bNumEntries
                    tmp.writeShort(0);//wFlags

                    for (int i = first; i <= last; i++) {
                        tmp.writeByte((imgRGBs[i] >>> 16) & 0xff); // red
                        tmp.writeByte((imgRGBs[i] >>> 8) & 0xff); // green
                        tmp.writeByte(imgRGBs[i] & 0xff); // blue
                        tmp.writeByte(0); // reserved*/
                    }

                }
                break;
            }
            case 8: {
                //IndexColorModel imgPalette = (IndexColorModel) image.getColorModel();
                int[] imgRGBs = new int[256];
                imgPalette.getRGBs(imgRGBs);
                int[] previousRGBs = new int[256];
                if (vt.previousPalette != null) {
                    vt.previousPalette.getRGBs(previousRGBs);
                }
                if (isKeyframe||!Arrays.equals(imgRGBs, previousRGBs)) {
                    paletteChange=true;
                    vt.previousPalette = imgPalette;
                    /*
                    int first = imgPalette.getMapSize();
                    int last = -1;
                    for (int i = 0; i < 16; i++) {
                    if (previousRGBs[i] != imgRGBs[i] && i < first) {
                    first = i;
                    }
                    if (previousRGBs[i] != imgRGBs[i] && i > last) {
                    last = i;
                    }
                    }*/
                    int first = 0;
                    int last = imgPalette.getMapSize() - 1;
                    /*
                     * typedef struct {
                    BYTE         bFirstEntry;
                    BYTE         bNumEntries;
                    WORD         wFlags;
                    PALETTEENTRY peNew[];
                    } AVIPALCHANGE;
                     *
                     * typedef struct tagPALETTEENTRY {
                    BYTE peRed;
                    BYTE peGreen;
                    BYTE peBlue;
                    BYTE peFlags;
                    } PALETTEENTRY;
                     */
                    tmp = new ByteArrayImageOutputStream(ByteOrder.LITTLE_ENDIAN);
                    tmp.writeByte(first);//bFirstEntry
                    tmp.writeByte(last - first + 1);//bNumEntries
                    tmp.writeShort(0);//wFlags
                    for (int i = first; i <= last; i++) {
                        tmp.writeByte((imgRGBs[i] >>> 16) & 0xff); // red
                        tmp.writeByte((imgRGBs[i] >>> 8) & 0xff); // green
                        tmp.writeByte(imgRGBs[i] & 0xff); // blue
                        tmp.writeByte(0); // reserved*/
                    }
                }

                break;
            }
        }
        if (tmp != null) {
            tmp.close();
            writePalette(track, tmp.toByteArray(), 0, (int) tmp.length(), isKeyframe);
        }
        return paletteChange;
    }

    private Codec createCodec(Format fmt) {

        Codec[] codecs = Registry.getInstance().getEncoders(new Format(FormatKeys.MimeTypeKey, FormatKeys.MIME_AVI).append(fmt));
        return codecs.length == 0 ? null : codecs[0];
    }

    private void createCodec(Track tr) {
        Format fmt = tr.format;
        tr.codec = createCodec(fmt);
        String enc = fmt.get(FormatKeys.EncodingKey);
        if (tr.codec != null) {
            if (fmt.get(FormatKeys.MediaTypeKey) == FormatKeys.MediaType.VIDEO) {
                tr.codec.setInputFormat(new Format(FormatKeys.EncodingKey, VideoFormatKeys.ENCODING_BUFFERED_IMAGE, VideoFormatKeys.DataClassKey, BufferedImage.class).append(fmt));
                if (null == tr.codec.setOutputFormat(new Format(VideoFormatKeys.FixedFrameRateKey, true).append(fmt))) {
                    throw new UnsupportedOperationException("Track " + tr + " codec does not support format " + fmt + ". codec=" + tr.codec);
                }
            } else {
                tr.codec.setInputFormat(null);
                if (null == tr.codec.setOutputFormat(fmt)) {
                    throw new UnsupportedOperationException("Track " + tr + " codec " + tr.codec + " does not support format. " + fmt);
                }
            }
        }
    }

    public boolean isVFRSupported() {
        return false;
    }
}
