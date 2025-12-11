package org.bsc.javelit;

import io.javelit.components.media.ImageComponent;
import io.javelit.core.Jt;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.bsc.langgraph4j.GraphRepresentation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

public interface JtPlantUMLImage {

    static Optional<ImageComponent.Builder> build( GraphRepresentation representation  ) {

        if( representation == null || representation.content() == null ) {
            Jt.error("plantuml representation is null").use();
            return Optional.empty();
        }

        var reader = new SourceStringReader(representation.content());

        // Output the image to a file and capture its description.
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(1024 * 1024 )) {
            reader.outputImage(out, new FileFormatOption(FileFormat.PNG, true ) );
            var base64Image = Base64.getEncoder().encodeToString(out.toByteArray());
            var url =  "data:%s;base64,%s".formatted( FileFormat.PNG.getMimeType(),  base64Image );

            return Optional.of(Jt.image( url ));
        } catch (IOException e) {
            Jt.error("error processing plantuml representation %s".formatted(e.getMessage())).use();
            return Optional.empty();

        }
    }

}
