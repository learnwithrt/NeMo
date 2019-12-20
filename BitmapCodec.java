/*
 * @(#)BitmapCodec.java  1.0  2011-09-04
 * 
 * Copyright (c) 2011 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
import java.awt.image.BufferedImage;

/**
 * Converts BufferedImage to BitmapImage. 
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-09-04 Created.
 */
public class BitmapCodec extends AbstractVideoCodec {

    public BitmapCodec() {
        super(new Format[]{
                    new Format(AmigaVideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, FormatKeys.MimeTypeKey, AmigaVideoFormatKeys.MIME_ANIM,
                    AmigaVideoFormatKeys.EncodingKey, AmigaVideoFormatKeys.ENCODING_ANIM_OP5, AmigaVideoFormatKeys.DataClassKey, byte[].class, AmigaVideoFormatKeys.FixedFrameRateKey, false), //
                },
                new Format[]{
                    new Format(AmigaVideoFormatKeys.MediaTypeKey, FormatKeys.MediaType.VIDEO, AmigaVideoFormatKeys.MimeTypeKey, AmigaVideoFormatKeys.MIME_JAVA, 
                            AmigaVideoFormatKeys.EncodingKey, AmigaVideoFormatKeys.ENCODING_BUFFERED_IMAGE, AmigaVideoFormatKeys.FixedFrameRateKey, false), //
                });
        name="ILBM Codec";
    }
    @Override
    public int process(Buffer in, Buffer out) {
        out.setMetaTo(in);
        if (in.isFlag(BufferFlag.DISCARD)) {
            return CODEC_OK;
        }
        out.format=outputFormat;

        BufferedImage pixmap = (BufferedImage) in.data;
        Format vf = (Format) outputFormat;
        BitmapImage bitmap = out.data instanceof BitmapImage ? (BitmapImage) out.data : null;
        if (bitmap == null || bitmap.getWidth() != vf.get(AmigaVideoFormatKeys.WidthKey)
                || bitmap.getHeight() != vf.get(AmigaVideoFormatKeys.HeightKey) || bitmap.getDepth() != vf.get(AmigaVideoFormatKeys.DepthKey)) {
            bitmap = new BitmapImage(vf.get(AmigaVideoFormatKeys.WidthKey), vf.get(AmigaVideoFormatKeys.HeightKey), vf.get(AmigaVideoFormatKeys.DepthKey), pixmap.getColorModel());
            out.data = bitmap;
        }
        bitmap.setPlanarColorModel(pixmap.getColorModel());
        bitmap.convertFromChunky(pixmap);


        return CODEC_OK;
    }
}
