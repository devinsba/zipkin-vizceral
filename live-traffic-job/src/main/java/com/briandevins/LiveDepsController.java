package com.briandevins;

import com.briandevins.vizceral.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class LiveDepsController {

    @Value("${application.env:nonprod}")
    private String appEnv;

    @RequestMapping(path = "/live")
    public Node live() {
        return SpanSummaryRegistry.getNode();
    }
}
