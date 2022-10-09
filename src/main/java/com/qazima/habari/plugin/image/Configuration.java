package com.qazima.habari.plugin.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

@JsonPropertyOrder(alphabetic = true)
public class Configuration extends com.qazima.habari.plugin.core.Configuration {
    @Getter
    @Setter
    @JsonProperty("grayscaleParameterName")
    private String grayscaleParameterName = "(grayscale|gs)";

    @Getter
    @Setter
    @JsonProperty("heightParameterName")
    private String heightParameterName = "(height|h)";

    @Getter
    @Setter
    @JsonProperty("path")
    private String path = "./";

    @Getter
    @Setter
    @JsonProperty("rotateParameterName")
    private String rotateParameterName = "(rotate|r)";

    @Getter
    @Setter
    @JsonProperty("widthParameterName")
    private String widthParameterName = "(width|w)";
}
