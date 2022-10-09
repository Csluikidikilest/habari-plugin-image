package com.qazima.habari.plugin.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.qazima.habari.plugin.core.Content;
import com.sun.net.httpserver.HttpExchange;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpStatus;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@JsonTypeName("com.qazima.habari.plugin.image.Plugin")
public class Plugin extends com.qazima.habari.plugin.core.Plugin {
    @Getter
    @Setter
    @JsonProperty("configuration")
    private com.qazima.habari.plugin.image.Configuration configuration;

    private boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    private String getImageFormatName(File file) throws IOException {
        String formatName = "";

        ImageInputStream imageInputStream = ImageIO.createImageInputStream(file);
        Iterator iterator = ImageIO.getImageReaders(imageInputStream);
        if (iterator.hasNext()) {
            ImageReader imageReader = (ImageReader) iterator.next();
            formatName = imageReader.getFormatName();
        }
        imageInputStream.close();

        return formatName;
    }

    private Map<String, String> splitQuery(String parameters) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        if(!isNullOrEmpty(parameters)) {
            String[] pairs = parameters.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if(idx > 0) {
                    query_pairs.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
                } else {
                    query_pairs.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "0");
                }
            }
        }
        return query_pairs;
    }

    public int process(HttpExchange httpExchange, Content content) {
        String localPath = getConfiguration().getPath();
        String remotePath = Pattern.compile(getConfiguration().getUri()).matcher(httpExchange.getRequestURI().getPath().replace('/', File.separatorChar)).replaceAll("$2");
        String fileName = Path.of(localPath, remotePath).toString();
        String queryString = httpExchange.getRequestURI().getQuery();

        File file = new File(fileName);
        if (file.exists()) {
            try {
                Map<String, String> parameters = splitQuery(queryString);
                content.setType(URLConnection.guessContentTypeFromName(file.getName()));
                content.setStatusCode(HttpStatus.SC_OK);
                BufferedImage srcBufferedImage = ImageIO.read(file);

                int bufferedImageType = BufferedImage.TYPE_INT_RGB;
                int srcHeight = srcBufferedImage.getHeight();
                int srcWidth = srcBufferedImage.getWidth();
                int dstHeight = srcBufferedImage.getHeight();
                int dstWidth = srcBufferedImage.getWidth();
                int dstAngle = 0;
                boolean forceRotate = false;
                boolean forceHeight = false;
                boolean forceWidth = false;

                Optional<String> prmGrayscale = parameters.keySet().stream().filter(key -> Pattern.compile(getConfiguration().getGrayscaleParameterName()).matcher(key).matches()).findFirst();
                if(prmGrayscale.isPresent()) {
                    bufferedImageType = BufferedImage.TYPE_BYTE_GRAY;
                }

                Optional<String> prmHeight = parameters.keySet().stream().filter(key -> Pattern.compile(getConfiguration().getHeightParameterName()).matcher(key).matches()).findFirst();
                if (prmHeight.isPresent()) {
                    dstHeight = Integer.parseInt(parameters.get(prmHeight.get()));
                    forceHeight = true;
                }

                Optional<String> prmWidth = parameters.keySet().stream().filter(key -> Pattern.compile(getConfiguration().getWidthParameterName()).matcher(key).matches()).findFirst();
                if (prmWidth.isPresent()) {
                    dstWidth = Integer.parseInt(parameters.get(prmWidth.get()));
                    forceWidth = true;
                }

                Optional<String> prmAngle = parameters.keySet().stream().filter(key -> Pattern.compile(getConfiguration().getRotateParameterName()).matcher(key).matches()).findFirst();
                if (prmAngle.isPresent()) {
                    dstAngle = Integer.parseInt(parameters.get(prmAngle.get()));
                    forceRotate = true;
                }

                if(forceHeight && !forceWidth) {
                    dstWidth = dstHeight * srcWidth / srcHeight;
                }

                if(forceWidth && !forceHeight) {
                    dstHeight = srcHeight * dstWidth / srcWidth;
                }

                BufferedImage dstBufferedImage = new BufferedImage(dstWidth, dstHeight, bufferedImageType);
                Graphics2D graphics2D = dstBufferedImage.createGraphics();
                if(forceRotate) {
                    graphics2D.rotate(Math.toRadians(dstAngle), dstWidth / 2, dstHeight / 2);
                }
                graphics2D.drawImage(srcBufferedImage, 0, 0, dstWidth, dstHeight, null);
                graphics2D.dispose();

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(dstBufferedImage, getImageFormatName(file), byteArrayOutputStream);
                content.setBody(byteArrayOutputStream.toByteArray());
            } catch (Exception e) {
                content.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                content.setType("text/plain");
                content.setBody(e.toString().getBytes(StandardCharsets.UTF_8));
            }
        } else {
            content.setStatusCode(HttpStatus.SC_NOT_FOUND);
            content.setType("text/plain");
            content.setBody("".getBytes(StandardCharsets.UTF_8));
        }

        return content.getStatusCode();
    }
}
