package com.target.devicemanager.components.printer.entities;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        visible = true)

@JsonSubTypes({
        @JsonSubTypes.Type(value = TextContent.class, name = "TEXT"),
        @JsonSubTypes.Type(value = BarcodeContent.class, name = "BARCODE"),
        @JsonSubTypes.Type(value = ImageContent.class, name = "IMAGE")
})

public abstract class PrinterContent {
    @Schema(description = "Three types are possible, their models are defined in the models section at the bottom of the page.")
    public ContentType type;
    @Schema(example = "Test Print\n\n\n From POSSUM\u001B|100fP")
    public String data;

    public void setType(ContentType type) {
        this.type = type;
    }

    public void setData(String data) {
        this.data = data;
    }
}


