/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.quarkiverse.snappy.it;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.xerial.snappy.Snappy;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;

@Path("/snappy")
@ApplicationScoped
public class SnappyResource {

    @GET
    public String hello() {
        return "Hello snappy";
    }

    @POST
    @Path("/roundtrip")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String roundtrip(String input) {
        try {
            byte[] compressed = Snappy.compress(input.getBytes(StandardCharsets.UTF_8));
            byte[] decompressed = Snappy.uncompress(compressed);
            return new String(decompressed, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @POST
    @Path("/stream-roundtrip")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String streamRoundtrip(String input) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (SnappyOutputStream snappyOut = new SnappyOutputStream(buffer)) {
                snappyOut.write(input.getBytes(StandardCharsets.UTF_8));
            }

            try (SnappyInputStream snappyIn = new SnappyInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
                return new String(snappyIn.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @POST
    @Path("/binary-roundtrip")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String binaryRoundtrip(String base64Input) {
        try {
            byte[] original = Base64.getDecoder().decode(base64Input);
            byte[] compressed = Snappy.compress(original);
            byte[] decompressed = Snappy.uncompress(compressed);
            return Base64.getEncoder().encodeToString(decompressed);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
