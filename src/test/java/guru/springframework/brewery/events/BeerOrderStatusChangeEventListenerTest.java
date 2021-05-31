package guru.springframework.brewery.events;

import com.github.jenspiegsa.wiremockextension.Managed;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import guru.springframework.brewery.domain.BeerOrder;
import guru.springframework.brewery.domain.OrderStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

// bring in the WireMock extension for JUnit
@ExtendWith(WireMockExtension.class)
class BeerOrderStatusChangeEventListenerTest {

    // Bringing in WireMock server - it will use a dynamic port, 
    // @Managed - it's managed by the WireMockExtension
    @Managed
    WireMockServer wireMockServer = with(wireMockConfig().dynamicPort());

    BeerOrderStatusChangeEventListener listener;

    @BeforeEach
    void setUp() {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        listener = new BeerOrderStatusChangeEventListener(restTemplateBuilder);

    }

    @Test
    void listen() {
        
        // configuring WireMock server to accept a post for an /update call + return OK 
        // in the console we get a 200 OK coming back from Netty>our web hook is hitting the WireMock server
        // syntax is similar to Mockito
        wireMockServer.stubFor(post("/update").willReturn(ok())); 

        BeerOrder beerOrder = BeerOrder.builder()
                    // we want to emulate it going from NEW to READY
                    .orderStatus(OrderStatusEnum.READY) 
                // we want a call on our client, wireMockServer.port() - uses dynamic port which is set at runtime
                .orderStatusCallbackUrl("http://localhost:" + wireMockServer.port() + "/update") 
                .createdDate(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        // we pass in the status of NEW 
        BeerOrderStatusChangeEvent event = new BeerOrderStatusChangeEvent(beerOrder, OrderStatusEnum.NEW);

        listener.listen(event);

        // urlEqualTo = a WireMock option>we have a matcher here for the URL>we verify the WireMock stub is called once
        verify(1, postRequestedFor(urlEqualTo("/update")));

    }
}
