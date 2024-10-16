package org.example.booksservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.SneakyThrows;
import org.example.booksservice.dto.request.BookDto;
import org.example.booksservice.dto.response.BookResponse;
import org.example.booksservice.dto.response.BookResponseWithReview;
import org.example.booksservice.dto.response.ReviewRequest;
import org.example.booksservice.entity.Book;
import org.example.booksservice.repository.BookRepository;
import org.example.booksservice.service.BookService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    private final Cloudinary cloudinary;
    private final BookRepository bookRepository;
    private final WebClient webClient;

    public BookServiceImpl(Cloudinary cloudinary, BookRepository bookRepository, WebClient.Builder webClientBuilder) {
        this.cloudinary = cloudinary;
        this.bookRepository = bookRepository;
        this.webClient = webClientBuilder.build();
    }

    @SneakyThrows
    @Transactional
    public Book createBook(BookDto bookDto) {
        Optional<Book> book = bookRepository.findByTitle(bookDto.getTitle());
        if (book.isPresent()) {
            throw new IllegalArgumentException("Эта книга уже присутствует");
        }

        Map<String, Object> uploadImg = cloudinary.uploader().upload(bookDto.getImgUrl().getBytes(), ObjectUtils.emptyMap());
        Map<String, Object> uploadPdf = cloudinary.uploader().upload(bookDto.getPdfUrl().getBytes(),
                ObjectUtils.asMap("resource_type", "raw"));

        Book newBook = Book.builder()
                .genre(bookDto.getGenre())
                .title(bookDto.getTitle())
                .description(bookDto.getDescription())
                .pdfUrl(uploadPdf.get("url").toString())
                .author(bookDto.getAuthor())
                .imgUrl(uploadImg.get("url").toString())
                .createdAt(LocalDateTime.now())
                .build();


        String genre=getSubscribers();
        if (genre.equalsIgnoreCase(newBook.getGenre())){
            sendMessage();
        }
        return bookRepository.save(newBook);
    }

    public BookResponseWithReview getBookWithTitle(String title) {
        Book book = bookRepository.findByTitle(title)
                .orElseThrow(() -> new IllegalArgumentException("Книга с таким названием не найдена"));

        saveDownload(book.getId(), getUserId());

        BookResponse bookResponse = mapBookToResponse(book);
        ReviewRequest review = getReview(book.getId());

        return BookResponseWithReview.builder()
                .bookResponse(bookResponse)
                .review(review)
                .build();
    }


    public BookResponse getBookWithAuthor(String author) {
        Book book = bookRepository.findAllByAuthor(author)
                .orElseThrow(() -> new IllegalArgumentException("Книга с таким автором не найдена"));

        return mapBookToResponse(book);
    }

    public BookResponse getBookWithGenre(String genre) {
        Book book = bookRepository.findAllByGenre(genre)
                .orElseThrow(() -> new IllegalArgumentException("Книга с таким жанром не найдена"));

        return mapBookToResponse(book);
    }

    private BookResponse mapBookToResponse(Book book) {
        return BookResponse.builder()
                .title(book.getTitle())
                .author(book.getAuthor())
                .description(book.getDescription())
                .genre(book.getGenre())
                .imgUrl(book.getImgUrl())
                .pdfUrl(book.getPdfUrl())
                .build();
    }
    private void saveDownload(Long bookId,Long userId){
        webClient.post()
                .uri("http://localhost:8081/api/v1/downloads/{bookId}/{userId}", bookId, userId)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> {
                    System.out.println("Ошибка при сохранении загрузки: " + error.getMessage());
                })
                .subscribe();

    }
    public BookResponse saveWishList(Long bookId){
        webClient.post()
                .uri("http://localhost:8083/api/v1/wishlist/{bookId}/{userId}",bookId,getUserId())
                .retrieve()
                .bodyToMono(BookResponse.class)
                .subscribe();
        return null;
    }

    public List<BookResponse> getWishListBooks(Long userId) {
        List<Long> downloadedBookIds = webClient.get()
                .uri("http://localhost:8083/api/v1/wishlist/get-wish/{userId}", userId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Long>>() {})
                .block();
        return getBooksByIds(downloadedBookIds);
    }

    public List<BookResponse> getDownloadedBooks(Long userId) {
        List<Long> downloadedBookIds = webClient.get()
                .uri("http://localhost:8081/api/v1/downloads/getDownloads/{userId}", userId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Long>>() {})
                .block();
        return getBooksByIds(downloadedBookIds);
    }

    public List<BookResponse> getBooksByIds(List<Long> bookIds) {
        return bookRepository.findAllById(bookIds)
                .stream()
                .map(this::mapBookToResponse)
                .collect(Collectors.toList());
    }


    public Long getUserId(){
        Long userId=webClient.get()
                .uri("http://localhost:7070/api/v1/auth/get-user-id")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Long>() {
                })
                .block();
        return userId;
    }

    public void messageAboutSubscription(String genre) {
        webClient.post()
                .uri("http://localhost:8888/api/v1/subscription/{genre}/{userId}", genre, getUserId())
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> {
                    System.err.println("Error during subscription: " + error.getMessage());
                })
                .subscribe(response -> {
                    System.out.println("Subscription response: " + response);
                });
    }

    public String getSubscribers() {
        return webClient.get()
                .uri("http://localhost:8888/api/v1/subscription/{userId}", getUserId())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public void sendMessage(){
        webClient.post()
                .uri("http://localhost:7070/api/v1/auth/send-message")
                .retrieve()
                .bodyToMono(String.class)
                .subscribe();
    }




    public ReviewRequest getReview(Long bookId) {
        return webClient.get()
                .uri("http://localhost:8181/api/v1/review/get-review/{bookId}", bookId)
                .retrieve()
                .bodyToMono(ReviewRequest.class)
                .block();  // Блокирует выполнение до получения результата
    }





}
