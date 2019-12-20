/*
 * @(#)JPGCodec.java 
 *
 * Copyright (c) 2011 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */

import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

/**
 * {@code JPEGCodec} encodes a BufferedImage as a byte[] array.
 * <p>
 * Supported input formats:
 * <ul>
 * {@code VideoFormat} with {@code BufferedImage.class}, any width, any height,
 * any depth.
 * </ul>
 * Supported output formats:
 * <ul>
 * {@code VideoFormat} with {@code byte[].class}, same width and height as input
 * format, depth=24.
 * </ul>
 *
 * @author Werner Randelshofer
 * @version $Id: JPEGCodec.java 186 2012-03-28 11:18:42Z werner $
 */
public class JPEGCodec extends AbstractVideoCodec {

    public JPEGCodec() {
        super(new Format[]{
                    new Format(VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_JAVA,
                    VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_BUFFERED_IMAGE), //
                    new Format(VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_QUICKTIME,
                    VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_QUICKTIME_JPEG,//
                    VideoFormatKeys.CompressorNameKey, VideoFormatKeys.COMPRESSOR_NAME_QUICKTIME_JPEG, //
                    VideoFormatKeys.DataClassKey, byte[].class, VideoFormatKeys.DepthKey, 24), //
                    //
                    new Format(VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_AVI,
                    VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_MJPG, VideoFormatKeys.DataClassKey, byte[].class, VideoFormatKeys.DepthKey, 24), //
                },
                new Format[]{
                    new Format(VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_JAVA,
                    VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_BUFFERED_IMAGE), //
                    new Format(VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_QUICKTIME,//
                    VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_QUICKTIME_JPEG,//
                    VideoFormatKeys.CompressorNameKey, VideoFormatKeys.COMPRESSOR_NAME_QUICKTIME_JPEG, //
                    VideoFormatKeys.DataClassKey, byte[].class, VideoFormatKeys.DepthKey, 24), //
                    //
                    new Format(VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_AVI,
                    VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_MJPG, VideoFormatKeys.DataClassKey, byte[].class, VideoFormatKeys.DepthKey, 24), //
                }//
                );
        name = "JPEG Codec";
    }

    @Override
    public int process(Buffer in, Buffer out) {
        if (outputFormat.get(VideoFormatKeys.EncodingKey).equals(VideoFormatKeys.ENCODING_BUFFERED_IMAGE)) {
            return decode(in, out);
        } else {
            return encode(in, out);
        }
    }

    public int encode(Buffer in, Buffer out) {
        out.setMetaTo(in);
        out.format = outputFormat;
        if (in.isFlag(BufferFlag.DISCARD)) {
            return CODEC_OK;
        }
        BufferedImage image = getBufferedImage(in);
        if (image == null) {
            out.setFlag(BufferFlag.DISCARD);
            return CODEC_FAILED;
        }
        ByteArrayImageOutputStream tmp;
        if (out.data instanceof byte[]) {
            tmp = new ByteArrayImageOutputStream((byte[]) out.data);
        } else {
            tmp = new ByteArrayImageOutputStream();
        }

        try {
            ImageWriter iw = ImageIO.getImageWritersByMIMEType("image/jpeg").next();
            ImageWriteParam iwParam = iw.getDefaultWriteParam();
            iwParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            float quality = outputFormat.get(VideoFormatKeys.QualityKey, 1f);
            iwParam.setCompressionQuality(quality);
            iw.setOutput(tmp);
            IIOImage img = new IIOImage(image, null, null);
            iw.write(null, img, iwParam);
            iw.dispose();

            out.sampleCount = 1;
            out.setFlag(BufferFlag.KEYFRAME);
            out.data = tmp.getBuffer();
            out.offset = 0;
            out.length = (int) tmp.getStreamPosition();
            return CODEC_OK;
        } catch (IOException ex) {
            ex.printStackTrace();
            out.setFlag(BufferFlag.DISCARD);
            return CODEC_FAILED;
        }
    }

    public int decode(Buffer in, Buffer out) {
        out.setMetaTo(in);
        out.format = outputFormat;
        if (in.isFlag(BufferFlag.DISCARD)) {
            return CODEC_OK;
        }
        byte[] data = (byte[]) in.data;
        if (data == null) {
            out.setFlag(BufferFlag.DISCARD);
            return CODEC_FAILED;
        }
        ByteArrayImageInputStream tmp = new ByteArrayImageInputStream(data);

        try {
            // ImageReader ir = (ImageReader) ImageIO.getImageReadersByMIMEType("image/jpeg").next();
            ImageReader ir = new MJPGImageReader(new MJPGImageReaderSpi());
            ir.setInput(tmp);
            out.data = ir.read(0);
            ir.dispose();

            out.sampleCount = 1;
            out.offset = 0;
            out.length = (int) tmp.getStreamPosition();
            return CODEC_OK;
        } catch (IOException ex) {
            ex.printStackTrace();
            out.setFlag(BufferFlag.DISCARD);
            return CODEC_FAILED;
        }
    }
}
