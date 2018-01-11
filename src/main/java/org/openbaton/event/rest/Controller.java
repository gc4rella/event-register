package org.openbaton.event.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.openbaton.catalogue.mano.record.NetworkServiceRecord;
import org.openbaton.catalogue.mano.record.VirtualNetworkFunctionRecord;
import org.openbaton.catalogue.nfvo.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

  private Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @RequestMapping(value = "/event", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
  public void receiveEvent(@RequestBody String msg) {
    JsonObject event = new JsonParser().parse(msg).getAsJsonObject();
    log.info("Received event from NFVO:\n" + event);
    if (event.has("action")) {
      String action = event.get("action").getAsString();
      log.info("The action is: " + action);

      JsonObject payload = event.get("payload").getAsJsonObject();
      switch (Action.valueOf(action)) {
        case INSTANTIATE_FINISH: // this event is only related to NSR
          NetworkServiceRecord networkServiceRecord = gson.fromJson(payload, NetworkServiceRecord.class);
          log.info(String.format("Received INSTANTIATE_FINISH with payload: \n\n %s\n", payload));
          log.debug(networkServiceRecord.toString());
          break;
        case START:
          VirtualNetworkFunctionRecord virtualNetworkFunctionRecord = gson.fromJson(payload, VirtualNetworkFunctionRecord.class);
          log.info(String.format("Received INSTANTIATE_FINISH with payload: \n\n %s\n", payload));
          log.debug(virtualNetworkFunctionRecord.toString());
          break;
      }

      /*
       * execute the module business logic
       */
    }
  }
}
