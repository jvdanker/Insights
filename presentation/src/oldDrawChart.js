import * as d3 from "d3";

function drawChart(data, config_size) {
    const min = d3.min(data, d => d.epoch);
    const max = d3.max(data, d => d.epoch);
    const plot_width = config_size.svg_width - config_size.plot_margin.left - config_size.plot_margin.right;
    const plot_height = config_size.svg_height - config_size.plot_margin.top - config_size.plot_margin.bottom;

    const svg = d3
        .select("svg")
        .attr("width",
            config_size.svg_width +
            config_size.plot_margin.right +
            config_size.plot_margin.left)
        .attr("height",
            config_size.svg_height +
            config_size.plot_margin.top +
            config_size.plot_margin.bottom);

    const plot_g = svg
        .append("g")
        .attr("transform","translate("+[
            config_size.plot_margin.left,
            config_size.plot_margin.top
        ]+")")

    const background = plot_g
        .append('rect')
        .attr('width', plot_width)
        .attr('height', plot_height)
        .attr('fill', '#ffffff')
        .attr('fill-opacity', '0');

    // const clippingRect = plot_g
    //     .append("clipPath")
    //     .attr("id", "clippy")
    //     .append("rect")
    //     .attr("width",config_size.svg_width)
    //     .attr("height",config_size.svg_height)
    //     .attr("fill","none")

    const x = d3.scaleTime()
        .range([0, plot_width])
        .domain([min, max]);
    // let xx = x.copy();

    const yScaleCommits = d3.scaleLinear()
        .range([plot_height,0])
        .domain([0, d3.max(data, d => d.count)]);

    // const yScaleCommitters = d3.scaleLinear()
    //     .range([0, plot_height/4])
    //     .domain([0, d3.max(data, d => d.committers)]);

    // const yScaleFiles = d3.scaleLinear()
    //     .range([(plot_height/2), plot_height/3])
    //     .domain([0, d3.max(data, d => d.files)]);

    const lineCountCommits = d3.line()
        .x(d => x(d.epoch)) // xx(d.epoch))
        .y(d => yScaleCommits(d.count))
        .curve(d3.curveMonotoneX);

    // const lineCountCommitters = d3.line()
    //     .x(d => x(d.epoch)) // xx(d.epoch))
    //     .yScaleCommits(d => yScaleCommitters(d.committers))
    //     .curve(d3.curveMonotoneX);
    //
    // const lineCountCommitsNormalized = d3.line()
    //     .x(d => x(d.epoch)) // xx(d.epoch))
    //     .yScaleCommits(d => yScaleCommits(d.countNormalized))
    //     .curve(d3.curveMonotoneX);
    //
    // const lineCountFiles = d3.line()
    //     .x(d => x(d.epoch)) // xx(d.epoch))
    //     .yScaleCommits(d => yScaleFiles(d.files))
    //     .curve(d3.curveMonotoneX);

    // const xAxis = ; // d3.axisBottom(xx);
    const xAxisG = plot_g
        .append("g")
        .attr('class', 'x.axis')
        .attr("transform","translate("+[0,plot_height] + ")")
        .call(d3.axisBottom(x));

    // const yAxis = ;
    const yAxisG = plot_g
        .append("g")
        .attr('class', 'y.axis')
        .call(d3.axisLeft(yScaleCommits))

    // const yAxis2 = d3.axisRight(yScaleCommitters);
    // const yAxisG2 = plot_g
    //     .append("g")
    //     .attr("transform", "translate(" + config_size.svg_width + ", 0)")
    //     .call(yAxis2)

    // const yAxisFiles = d3.axisRight(yScaleFiles);
    // const yAxisGFiles = plot_g
    //     .append("g")
    //     .attr("transform", "translate(" + config_size.svg_width + " ,0)")
    //     .call(yAxisFiles)

    // const pathCommitsNormalized = plot_g
    //     .append("path")
    //     .data([data])
    //     .attr("d", lineCountCommitsNormalized)
    //     .attr("clip-path","url(#clippy)")
    //     .attr("fill", "none")
    //     .attr("stroke", "#112944")
    //     .attr("opaque", 0.3)
    //     .attr("stroke-width", 2);

    plot_g
        .append("path")
        .datum(data)
        .attr('class', 'line')
        .attr("d", lineCountCommits)
        // .attr("clip-path","url(#clippy)")
        .attr("fill", "none")
        .attr("stroke", "#707f8d");

    plot_g
        .selectAll("myCircles")
        .data(data)
        .enter()
        .append("circle")
        .attr("fill", "#5c6bce")
        .attr("stroke", "none")
        .attr("cx", function(d) { return x(d.epoch) })
        .attr("cy", function(d) { return yScaleCommits(d.count) })
        .attr("r", 2);

    // const pathCommitters = plot_g
    //     .append("path")
    //     .data([data])
    //     .attr("d", lineCountCommitters)
    //     .attr("clip-path","url(#clippy)")
    //     .attr("fill", "none")
    //     .attr("stroke", "#6cb1ff")
    //     .attr("stroke-width", 2);

    // const pathFiles = plot_g
    //     .append("path")
    //     .data([data])
    //     .attr("d", lineCountFiles)
    //     .attr("clip-path","url(#clippy)")
    //     .attr("fill", "none")
    //     .attr("stroke", "#a235ff")
    //     .attr("stroke-width", 2);

    // const zoom = d3.zoom()
    //     .on("zoom", function(event) {
    //         xx = event.transform.rescaleX(x);
    //         xAxisG.call(xAxis.scale(xx));
    //
    //         pathCommits.attr("d", lineCountCommits);
    //         pathCommitters.attr("d", lineCountCommitters);
    //         pathCommitsNormalized.attr("d", lineCountCommitsNormalized);
    //         pathFiles.attr("d", lineCountFiles);
    //     });

    // svg.call(zoom);

    return [svg, plot_g, x, yScaleCommits];
}