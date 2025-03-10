package org.lucidant.springboot.todo;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lucidant.springboot.jpa.entity.Todo;
import org.lucidant.springboot.jpa.repo.ToDoRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Here we use a MockWebServer to test a controller endpoint which uses a webclient to contact another
 * web service. Without the MockWebServer we end up with countless unhelpful mocks like this.</p>
 *
 * <pre>
 *           when(webClientMock.get())
 *           .thenReturn(requestHeadersUriSpecMock);
 *         when(requestHeadersUriMock.uri("/employee/{id}", employeeId))
 *           .thenReturn(requestHeadersSpecMock);
 *         when(requestHeadersMock.retrieve())
 *           .thenReturn(responseSpecMock);
 * </pre>
 *
 * With MockWebServer we push the mock from the Spring WebClient to the WebServer. MockWebServer does what it says on
 * the tin.
 * <a href="https://www.baeldung.com/mockserver#Verifying%20Requests">MockWebServer</a>
 * <p>The job now becomes a bit like using Mockito</p>
 * <ol>
 *     <li>(when) enqueue a response when an endpoint is hit</li>
 *     <li>(verify) verify that the request was received as expected</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class TodoControllerTest {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static MockWebServer mockBackEnd;

	@Mock
	private ToDoRepository toDoRepository;

	private TodoController controller;

	private static int port;

	@BeforeAll
	static void beforeAll() throws IOException {
		mockBackEnd = new MockWebServer();
		mockBackEnd.start();

		port = mockBackEnd.getPort();
	}

	@BeforeEach
	void beforeEach() {
		final var baseUrl = String.format("http://localhost:%s", port);

		final var webClient = WebClient.builder()
			.baseUrl(baseUrl)
			.build();

		var exchangeAdapter =  WebClientAdapter.create(webClient);
		var factory = HttpServiceProxyFactory
				.builder()
				.exchangeAdapter(exchangeAdapter)
				.build();
		var todoClient = factory.createClient(TodoClient.class);

		controller = new TodoController(todoClient, toDoRepository);
	}

	@AfterAll
	static void afterAll() throws IOException {
		mockBackEnd.shutdown();
	}

	@Test
	void testGetMono() throws JsonProcessingException, InterruptedException {
		final var mockTodo = Todo.builder()
			.id(1)
			.title("Initial Title")
			.completed(false)
			.userId(100)
			.build();

		mockBackEnd.enqueue(new MockResponse()
			.setBody(objectMapper.writeValueAsString(mockTodo))
			.addHeader("Content-Type", "application/json"));

		Mono<Todo> todoMono = controller.getTodoByIdMono(1);

		StepVerifier.create(todoMono)
			.expectNextMatches(todo -> todo.getTitle().equals("Initial Title"))
			.verifyComplete();

		final var recordedRequest = mockBackEnd.takeRequest();
		assertEquals("GET", recordedRequest.getMethod());
		assertEquals("/todos/1", recordedRequest.getPath());
	}

	@Test
	void testGet() throws JsonProcessingException, InterruptedException {
		final var mockTodo = Todo.builder()
			.id(1)
			.title("Initial Title")
			.completed(false)
			.userId(100)
			.build();

		mockBackEnd.enqueue(new MockResponse()
			.setBody(objectMapper.writeValueAsString(mockTodo))
			.addHeader("Content-Type", "application/json"));

		final Todo todo = controller.getTodoById(1);

		StepVerifier.create(Mono.just(todo))
			.expectNextMatches(result -> result.getTitle().equals("Initial Title"))
			.verifyComplete();

		final var recordedRequest = mockBackEnd.takeRequest();
		assertEquals("GET", recordedRequest.getMethod());
		assertEquals("/todos/1", recordedRequest.getPath());
	}
}
