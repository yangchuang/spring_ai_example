package spring.ai.example.controller;

import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class RAGController {

    private final String template = """
                        
            You're a DOCUMENT assistant.
            Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
            If unsure, simply state that you don't know.
                    
            DOCUMENTS:
            {documents}
                        
            """;

    private final OpenAiChatClient chatClient;

    private final VectorStore vectorStore;

    private final TokenTextSplitter tokenTextSplitter;


    @Autowired
    public RAGController(OpenAiChatClient chatClient, VectorStore vectorStore, TokenTextSplitter tokenTextSplitter) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
        this.tokenTextSplitter = tokenTextSplitter;
    }

    @GetMapping("/ai/doc_ask")
    public String doc_ask(@RequestParam(value = "message", defaultValue = "Hi") String message) {
        List<Document> listOfSimilarDocuments = vectorStore.similaritySearch(message);
        String documents = listOfSimilarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining(System.lineSeparator()));

        Message systemMessage = new SystemPromptTemplate(this.template)
                .createMessage(Map.of("documents", documents));
        UserMessage userMessage = new UserMessage(message);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        ChatResponse aiResponse = chatClient.call(prompt);
        return aiResponse.getResult().getOutput().getContent();
    }

    /**
     * example
     * */
    @PostMapping("/ai/doc_upload")
    public Map doc_upload(MultipartFile file) throws IOException {
        File localFile = convertMultiPartToFile(file);
//        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
//                .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder().withNumberOfBottomTextLinesToDelete(3)
//                        .withNumberOfTopPagesToSkipBeforeDelete(1)
//                        .build())
//                .withPagesPerDocument(1)
//                .build();


        //var pdfReader = new PagePdfDocumentReader(new FileSystemResource(localFile), config);
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(new FileSystemResource(localFile));
        TokenTextSplitter textSplitter = new TokenTextSplitter();
        vectorStore.accept(textSplitter.apply(pdfReader.get()));

        return Map.of("result", "upload done");
    }


    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        //store upload file to somewhere on server
        File convFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }

}
