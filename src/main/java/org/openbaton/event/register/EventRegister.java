package org.openbaton.event.register;

import org.openbaton.catalogue.nfvo.Action;
import org.openbaton.catalogue.nfvo.EndpointType;
import org.openbaton.catalogue.nfvo.EventEndpoint;
import org.openbaton.sdk.NFVORequestor;
import org.openbaton.sdk.NfvoRequestorBuilder;
import org.openbaton.sdk.api.exception.SDKException;
import org.openbaton.sdk.api.rest.EventAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextClosedEvent;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@ConfigurationProperties
@ComponentScan("org.openbaton.event")
public class EventRegister implements ApplicationListener<ContextClosedEvent>, CommandLineRunner{
  private NFVORequestor requestor;
  private List<EventEndpoint> eventEndpoints = new ArrayList<>();
  @Value("${event.register.service.active:false}")
  private boolean service;
  @Value("${event.register.service.key:}")
  private String serviceKey;

  public boolean isService() {
    return service;
  }

  public void setService(boolean service) {
    this.service = service;
  }

  public String getServiceKey() {
    return serviceKey;
  }

  public void setServiceKey(String serviceKey) {
    this.serviceKey = serviceKey;
  }

  @Override
  public void run(String... args) throws Exception {

    /*
    This is the NFVO port
   */
    int obNfvoPort = 8080;
    /*
    This is the NFVO Ip
   */
    String obNfvoIp = "127.0.0.1";
    /*
    This must be true if during the NFVO installation the ssl was enabled
   */
    boolean isSslEnabled = false;
    /*
    This is the Project ID used to connect to the NFVO
   */
    String obProjectName = "default";
    /*
    This is the Password used to connect to the NFVO
   */
    String obPassword = "openbaton";
    /*
    This is the Username used to connect to the NFVO
   */
    String obUsername = "admin";

    String serviceName = "event-register";

    if (!service) {
      requestor =
          NfvoRequestorBuilder.create()
              .nfvoIp(obNfvoIp)
              .nfvoPort(obNfvoPort)
              .username(obUsername)
              .password(obPassword)
              .projectName(obProjectName)
              .sslEnabled(isSslEnabled)
              .version("1")
              .build();
    } else {
      requestor =
        NfvoRequestorBuilder.create()
            .nfvoIp(obNfvoIp)
            .nfvoPort(obNfvoPort)
            .projectName(obProjectName)
            .serviceName(serviceName)
            .serviceKey(serviceKey)
            .sslEnabled(isSslEnabled)
            .version("1")
            .build();
    }
    /*
      Now the Event Agent needs to be retrieved
     */
    EventAgent eventAgent = requestor.getEventAgent();

    /*
      Define your endpoint
     */
    EventEndpoint eventEndpoint = new EventEndpoint();
    eventEndpoint.setName("MyEvent");
    eventEndpoint.setDescription("My event endpoint");
    if (service){
      eventEndpoint.setProjectId("*");
    }

    /*
      Register to all the event describing the correct instantiation of NSR
     */
    eventEndpoint.setEvent(Action.INSTANTIATE_FINISH);
    eventEndpoint.setType(EndpointType.REST);
    eventEndpoint.setEndpoint("http://127.0.0.1:8081/event");

    /*
    It is also possible to filter the event based on the NSR id or the VNFR id. Putting to null means you want to
    receive events for all NSR or VNFR. Please consider that the events refer or to a NSR or to a VNFR so you can
    only filter by NSR id the events that are related to NSR, same for the VNFRs.
     */
    eventEndpoint.setNetworkServiceId(null);
    eventEndpoint.setVirtualNetworkFunctionId(null);

    /*
      Now register the endpoint
     */
    try {
      eventEndpoints.add(eventAgent.create(eventEndpoint));
      eventEndpoint.setEvent(Action.RELEASE_RESOURCES);
      eventEndpoints.add(eventAgent.create(eventEndpoint));
      eventEndpoint.setEvent(Action.RELEASE_RESOURCES_FINISH);
      eventEndpoints.add(eventAgent.create(eventEndpoint));
      eventEndpoint.setEvent(Action.ERROR);
      eventEndpoints.add(eventAgent.create(eventEndpoint));
    } catch (SDKException e) {
      e.printStackTrace();
      System.err.println("Got an exception :(");
    }
  }

  public static void main(String[] args) {
    SpringApplication.run(EventRegister.class, args);
  }

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    eventEndpoints.forEach(e -> {
      try {
        requestor.getEventAgent().delete(e.getId());
      } catch (SDKException e1) {
        e1.printStackTrace();
      }
    });
  }
}
