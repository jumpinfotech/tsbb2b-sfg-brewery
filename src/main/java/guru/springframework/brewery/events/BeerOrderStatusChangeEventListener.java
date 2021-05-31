/*
 *  Copyright 2019 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package guru.springframework.brewery.events;

import guru.springframework.brewery.web.mappers.DateMapper;
import guru.springframework.brewery.web.model.OrderStatusUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
// if I have an order status change> I do a web hook callback> 
// simple>just send a post request to your URL 
public class BeerOrderStatusChangeEventListener {

    //Spring manages the RestTemplate 
    RestTemplate restTemplate;
    DateMapper dateMapper = new DateMapper();

    // I'm injecting in a RestTemplateBuilder
    public BeerOrderStatusChangeEventListener(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Async
    @EventListener
    public void listen(BeerOrderStatusChangeEvent event){
        System.out.println("I got an order status change event");
        System.out.println(event);

        // implemented the OrderStatusUpdate event>
        // I get the order + I'm basically doing a type conversion here 
        // - I'm using the builder pattern to create a OrderStatusUpdate object
        OrderStatusUpdate update = OrderStatusUpdate.builder()
                .id(event.getBeerOrder().getId())
                .orderId(event.getBeerOrder().getId())
                .version(event.getBeerOrder().getVersion() != null ? event.getBeerOrder().getVersion().intValue() : null)
                .createdDate(dateMapper.asOffsetDateTime(event.getBeerOrder().getCreatedDate()))
                .lastModifiedDate(dateMapper.asOffsetDateTime(event.getBeerOrder().getLastModifiedDate()))
                .orderStatus(event.getBeerOrder().getOrderStatus().toString())
                .customerRef(event.getBeerOrder().getCustomerRef())
                .build();

        // The 1st property I'm passing into postForObject is the callback URL which the event should have, 
        // the request is the OrderStatusUpdate object > that's the response I'm expecting back. 
        // Basic - a good response comes back.
        try{
            log.debug("Posting to callback url");
            restTemplate.postForObject(event.getBeerOrder().getOrderStatusCallbackUrl(), update, String.class);
        } catch (Throwable t){
            // if I get a non 200 response back from the call, then RestTemplate throws an exception 
            log.error("Error Preforming callback for order: " + event.getBeerOrder().getId(), t);
        }
    }
}
