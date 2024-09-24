package com.example.audio.controller;

import com.example.audio.service.AudioCutProgress;
import com.example.audio.service.AudioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/audio")
public class AudioFileController {

    @Autowired
    private AudioService audioService;

    // Endpoint para cortar o áudio
    @PostMapping("/cut/{fileName}")
    public ResponseEntity<?> cutAudio(
            @PathVariable("fileName") String fileName,
            @RequestParam("startSeconds") double startSeconds,
            @RequestParam("durationSeconds") double durationSeconds) {

        try {
            // Valida os parâmetros
            if (startSeconds < 0 || durationSeconds <= 0) {
                return ResponseEntity.badRequest().body("Parâmetros inválidos.");
            }

            // Chama o serviço de corte de áudio
            String outputFileName = audioService.cutAudioFile(fileName, startSeconds, durationSeconds);

            // Verifica se o arquivo foi gerado corretamente
            Path outputPath = Paths.get("C:/cortes/" + outputFileName);
            if (!Files.exists(outputPath)) {
                return ResponseEntity.status(404).body("Arquivo cortado não encontrado.");
            }

            // Retorna o arquivo de áudio cortado
            Resource resource = new UrlResource(outputPath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFileName + "\"")
                    .body(resource);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao processar o áudio: " + e.getMessage());
        }
    }


    // Endpoint para obter o progresso atual do corte e o nome da rádio
    @GetMapping("/progress")
    public ResponseEntity<Map<String, Object>> getCutProgress(@RequestParam String radioName) {
        int progress = audioService.getCutProgress().getProgress();
        Map<String, Object> response = new HashMap<>();
        response.put("progress", progress);
        response.put("radioName", radioName);

        return ResponseEntity.ok(response);
    }

    // Endpoint para pausar o corte de áudio
    @PostMapping("/pause")
    public ResponseEntity<String> pauseCut() {
        try {
            audioService.pauseCut();
            return ResponseEntity.ok("Corte pausado com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao pausar o corte: " + e.getMessage());
        }
    }

    // Endpoint para retomar o corte de áudio
    @PostMapping("/resume")
    public ResponseEntity<String> resumeCut() {
        try {
            audioService.resumeCut();
            return ResponseEntity.ok("Corte retomado com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao retomar o corte: " + e.getMessage());
        }
    }

    // Endpoint para cancelar o corte de áudio
    @PostMapping("/cancel")
    public ResponseEntity<String> cancelCut() {
        try {
            audioService.cancelCut();
            return ResponseEntity.ok("Corte cancelado com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao cancelar o corte: " + e.getMessage());
        }
    }

    // Método para fazer upload de um arquivo de áudio
    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("O arquivo de áudio está vazio.");
            }

            String fileName = audioService.saveAudioFile(file);
            Map<String, String> response = new HashMap<>();
            response.put("fileName", fileName);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Erro ao carregar o arquivo.");
        }
    }

    // Método para listar todos os arquivos de áudio na pasta audiopasta
    @GetMapping("/list")
    public ResponseEntity<List<String>> listAllAudioFilesInAudiopasta() {
        try {
            List<String> fileNames = audioService.listAudioFilesFromDirectory("C:/pastaudios");
            return ResponseEntity.ok(fileNames);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Método para listar todos os arquivos de áudio na pasta cortes
    @GetMapping("/list/cortes")
    public ResponseEntity<List<String>> listAllAudioFilesInCortes() {
        try {
            List<String> fileNames = audioService.listAudioFilesFromDirectory("C:/cortes");
            return ResponseEntity.ok(fileNames);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Método para fazer o download de um arquivo de áudio específico
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadAudio(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("C:/pastaudios").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Ajustar o cabeçalho para "inline" para permitir a reprodução no navegador
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}
