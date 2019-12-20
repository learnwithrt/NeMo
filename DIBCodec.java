/*
 * @(#)DIBCodec.java 
 *
 * Copyright © 2011-2012 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@code DIBCodec} encodes a BufferedImage as a Microsoft Device Independent 
 * Bitmap (DIB) into a byte array.
 * <p>
 * The DIB codec only works with the AVI file format. Other file formats, such
 * as QuickTime, use a different encoding for uncompressed video.
 * <p>
 * This codec currently only supports encoding from a {@code BufferedImage} into 
 * the file format. Decoding support may be added in the future.
 * <p>
 * This codec does not encode the color palette of an image. This must be done
 * separately.
 * <p>
 * The pixels of a frame are written row by row from bottom to top and from
 * the left to the right. 24-bit pixels are encoded as BGR.
 * <p>
 * Supported input formats:
 * <ul>
 * {@code Format} with {@code BufferedImage.class}, any width, any height,
 * depth=4.
 * </ul>
 * Supported output formats:
 * <ul>
 * {@code Format} with {@code byte[].class}, same width and height as input
 * format, depth=4.
 * </ul>
 *
 * @author Werner Randelshofer
 * @version $Id: DIBCodec.java 186 2012-03-28 11:18:42Z werner $
 */
public class DIBCodec extends AbstractVideoCodec {

    public DIBCodec() {
        super(new Format[]{
                    new Format(FormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, FormatKeys.MimeTypeKey, FormatKeys.MIME_JAVA, 
                            FormatKeys.EncodingKey, VideoFormatKeys.ENCODING_BUFFERED_IMAGE, VideoFormatKeys.FixedFrameRateKey, true), //
                },
                new Format[]{
                    new Format(FormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, FormatKeys.MimeTypeKey, VideoFormatKeys.MIME_AVI,
                    FormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_DIB, VideoFormatKeys.DataClassKey, byte[].class,
                            VideoFormatKeys.FixedFrameRateKey, true, VideoFormatKeys.DepthKey,4), //
                    new Format(FormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, FormatKeys.MimeTypeKey, VideoFormatKeys.MIME_AVI,
                    FormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_DIB, VideoFormatKeys.DataClassKey, byte[].class,
                            VideoFormatKeys.FixedFrameRateKey, true, VideoFormatKeys.DepthKey,8), //
                    new Format(FormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, FormatKeys.MimeTypeKey, VideoFormatKeys.MIME_AVI,
                    FormatKeys.EncodingKey, VideoFormatKeys.ENCODING_AVI_DIB, VideoFormatKeys.DataClassKey, byte[].class,
                            VideoFormatKeys.FixedFrameRateKey, true, VideoFormatKeys.DepthKey,24), //
                });
    }

    @Override
    public int process(Buffer in, Buffer out) {
        out.setMetaTo(in);
        out.format = outputFormat;
        if (in.isFlag(BufferFlag.DISCARD)) {
            return CODEC_OK;
        }

        SeekableByteArrayOutputStream tmp;
        if (out.data instanceof byte[]) {
            tmp = new SeekableByteArrayOutputStream((byte[]) out.data);
        } else {
            tmp = new SeekableByteArrayOutputStream();
        }

        // Handle sub-image
        // FIXME - Scanline stride must be a multiple of four.
        Rectangle r;
        int scanlineStride;
        if (in.data instanceof BufferedImage) {
            BufferedImage image = (BufferedImage) in.data;
            WritableRaster raster = image.getRaster();
            scanlineStride = raster.getSampleModel().getWidth();
            r = raster.getBounds();
            r.x -= raster.getSampleModelTranslateX();
            r.y -= raster.getSampleModelTranslateY();
            out.header=image.getColorModel();
        } else {
            r = new Rectangle(0, 0, outputFormat.get(VideoFormatKeys.WidthKey), outputFormat.get(VideoFormatKeys.HeightKey));
            scanlineStride = outputFormat.get(VideoFormatKeys.WidthKey);
            out.header=null;
        }

        try {
            switch (outputFormat.get(VideoFormatKeys.DepthKey)) {
                case 4: {
                    byte[] pixels = getIndexed8(in);
                    if (pixels == null) {
                        out.setFlag(BufferFlag.DISCARD);
                        return CODEC_OK;
                    }
                    writeKey4(tmp, pixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                    break;
                }
                case 8: {
                    byte[] pixels = getIndexed8(in);
                    if (pixels == null) {
                        out.setFlag(BufferFlag.DISCARD);
                        return CODEC_OK;
                    }
                    writeKey8(tmp, pixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                    break;
                }
                case 24: {
                    int[] pixels = getRGB24(in);
                    if (pixels == null) {
                        out.setFlag(BufferFlag.DISCARD);
                        return CODEC_OK;
                    }
                    writeKey24(tmp, pixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                    break;
                }
                default:
                    out.setFlag(BufferFlag.DISCARD);
                    return CODEC_OK;
            }

            out.setFlag(BufferFlag.KEYFRAME);
            out.data = tmp.getBuffer();
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

    /** Encodes a 4-bit key frame.
     *
     * @param out The output stream.
     * @param pixels The image data.
     * @param offset The offset to the first pixel in the data array.
     * @param width The width of the image in data elements.
     * @param scanlineStride The number to append to offset to get to the next scanline.
     */
    public void writeKey4(OutputStream out, byte[] pixels, int width, int height, int offset, int scanlineStride)
            throws IOException {

        byte[] bytes = new byte[width];
        for (int y = (height - 1) * scanlineStride; y >= 0; y -= scanlineStride) { // Upside down
            for (int x = offset, xx = 0, n = offset + width; x < n; x += 2, ++xx) {
                bytes[xx] = (byte) (((pixels[y + x] & 0xf) << 4) | (pixels[y + x + 1] & 0xf));
            }
            out.write(bytes);
        }

    }

    /** Encodes an 8-bit key frame.
     *
     * @param out The output stream.
     * @param pixels The image data.
     * @param offset The offset to the first pixel in the data array.
     * @param width The width of the image in data elements.
     * @param scanlineStride The number to append to offset to get to the next scanline.
     */
    public void writeKey8(OutputStream out, byte[] pixels, int width, int height, int offset, int scanlineStride)
            throws IOException {

        for (int y = (height - 1) * scanlineStride; y >= 0; y -= scanlineStride) { // Upside down
            out.write(pixels, y + offset, width);
        }
    }

    /** Encodes a 24-bit key frame.
     *
     * @param out The output stream.
     * @param pixels The image data.
     * @param offset The offset to the first pixel in the data array.
     * @param width The width of the image in data elements.
     * @param scanlineStride The number to append to offset to get to the next scanline.
     */
    public void writeKey24(OutputStream out, int[] pixels, int width, int height, int offset, int scanlineStride)
            throws IOException {
        int w3 = width * 3;
        byte[] bytes = new byte[w3]; // holds a scanline of raw image data with 3 channels of 8 bit data
        for (int xy = (height - 1) * scanlineStride + offset; xy >= offset; xy -= scanlineStride) { // Upside down
            for (int x = 0, xp = 0; x < w3; x += 3, ++xp) {
                int p = pixels[xy + xp];
                bytes[x] = (byte) (p); // Blue
                bytes[x + 1] = (byte) (p >> 8); // Green
                bytes[x + 2] = (byte) (p >> 16); // Red
            }
            out.write(bytes);
        }
    }
}
