/*
 * @(#)PNGCodec.java  
 *
 * Copyright (c) 2011-2012 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

/**
 * {@code PNGCodec} encodes a BufferedImage as a byte[] array..
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
 * @version $Id: PNGCodec.java 188 2012-03-28 14:03:19Z werner $
 */
public class PNGCodec extends AbstractVideoCodec {

    public PNGCodec() {
        super(new Format[]{
                    new Format(VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_JAVA,
                    VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_BUFFERED_IMAGE), //
                },
                new Format[]{
                    new Format(VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_QUICKTIME,
                    VideoFormatKeys.DepthKey, 24,
                    VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_QUICKTIME_PNG, VideoFormatKeys.DataClassKey, byte[].class), //
                    //
                    new Format(VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_AVI,
                    VideoFormatKeys.DepthKey, 24,
                    VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_PNG, VideoFormatKeys.DataClassKey, byte[].class), //
                });
    }

    @Override
    public Format setOutputFormat(Format f) {
        String mimeType = f.get(VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_QUICKTIME);
        if (mimeType != null && !mimeType.equals(VideoFormatKeys.MIME_AVI)) {
            return super.setOutputFormat(
                    new Format(VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_QUICKTIME,
                    VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_QUICKTIME_PNG, VideoFormatKeys.DataClassKey,
                    byte[].class, VideoFormatKeys.DepthKey, 24).append(f));
        } else {
            return super.setOutputFormat(
                    new Format(VideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, VideoFormatKeys.MimeTypeKey, VideoFormatKeys.MIME_AVI,
                    VideoFormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_PNG, VideoFormatKeys.DataClassKey,
                    byte[].class, VideoFormatKeys.DepthKey, 24).append(f));
        }
    }

    @Override
    public int process(Buffer in, Buffer out) {
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
            ImageWriter iw = ImageIO.getImageWritersByMIMEType("image/png").next();
            ImageWriteParam iwParam = iw.getDefaultWriteParam();
            iw.setOutput(tmp);
            IIOImage img = new IIOImage(image, null, null);
            iw.write(null, img, iwParam);
            iw.dispose();

            out.setFlag(BufferFlag.KEYFRAME);
            out.header = null;
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
}
