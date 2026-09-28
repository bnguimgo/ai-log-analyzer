package com.bnguimgo.ailoganalyzer.parser;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class LogReader {

    public List<String> read(Path path) throws IOException {
        return Files.readAllLines(path);
    }
}