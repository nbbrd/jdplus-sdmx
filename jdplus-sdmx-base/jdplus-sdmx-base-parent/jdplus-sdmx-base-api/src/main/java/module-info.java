module jdplus.sdmx.base.api {

    requires static lombok;
    requires static nbbrd.design;
    requires static nbbrd.service;
    requires static org.checkerframework.checker.qual;

    requires sdmxdl.api;
    requires jdplus.toolkit.base.tsp;
    requires nbbrd.io.base;

    exports demetra.tsp.extra.sdmx;
    exports demetra.tsp.extra.sdmx.file;
    exports demetra.tsp.extra.sdmx.web;

    provides demetra.timeseries.TsProvider with
            demetra.tsp.extra.sdmx.file.SdmxFileProvider,
            demetra.tsp.extra.sdmx.web.SdmxWebProvider;
}