package de.escalon.hypermedia.spring;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.WriterBasedJsonGenerator;
import org.junit.Test;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.*;

/**
 * Created by Dietrich on 07.02.2015.
 */
public class ExpandingOutputStreamTest {

    @Test
    public void write() throws IOException, HttpMessageNotWritableException {

        String sampleJsonLd = "{\n" +
                "  \"@context\": \"http://schema.org/\",\n" +
                "  \"@type\": \"Person\",\n" +
                "  \"name\": \"Dietrich Schulten\"\n" +
                "}";
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        Writer expanding = new OutputStreamWriter(new ExpandingOutputStream(result));
        expanding.write(sampleJsonLd);
    }

    class ExpandingOutputStream extends OutputStream {
        private final Writer writer;

        public ExpandingOutputStream(OutputStream out) {
            this.writer = new OutputStreamWriter(out);
        }

        @Override
        public void write(int b) throws IOException {

            IOContext ctxt = null;
            int features = 0;
            ObjectCodec codec = null;
            JsonGenerator gen = new WriterBasedJsonGenerator(ctxt, features, codec, writer);
//            Object jsonObject = JsonUtils.from;
//
//            Map context = new HashMap();
//            JsonLdOptions options = new JsonLdOptions();
//            Map<String, Object> compact = JsonLdProcessor.compact(jsonObject, context, options);
//
//            return JsonUtils.toPrettyString(compact);
        }

    }
}

