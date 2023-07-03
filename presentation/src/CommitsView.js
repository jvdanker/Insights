import React, {useEffect, useRef} from 'react';
import * as d3 from "d3";
import "./BarChart.css";
import addMouseOver from "./MouseOver";



// function updateRange(data, params) {
//     const [minX, maxX] = params;
//     const maxY = d3.max(data, d => minX <= d.epoch && d.epoch <= maxX ? d.count : NaN);
//
//     update(
//         x.copy().domain(params),
//         yScaleCommits.copy().domain([0, maxY]),
//         xAxisG,
//         yAxisG
//     );
// }
//
// function update(x, y, xAxis, yAxis) {
//     x.domain(d3.extent(data, function(d) { return d.date; }));
//     y.domain([0, d3.max(data, function(d) { return d.close; })]);
//
//     var svg = d3.select("body");
//
//     // console.log(svg.select('.line'));
//     svg.select(".line")
//         .attr("d", lineCountCommits(data));
//
//     // svg.select(".x.axis")
//     //     .call(xAxis);
//
//     // svg.select(".y.axis")
//     //     .call(yAxis);
// }

function drawChart(ref, data, config) {
    let svgHeight = config.svg_height;
    const margin = config.plot_margin;
    const plot_width = config.svg_width - margin.left - margin.right;
    const plot_height = svgHeight -margin.top - margin.bottom;

    const svg = d3
        .select(ref)
        .attr("width",
            config.svg_width +
            config.plot_margin.right +
            config.plot_margin.left)
        .attr("height",
            svgHeight +
            config.plot_margin.top +
            config.plot_margin.bottom);

    const plot_g = svg
        .append("g")
        .attr("transform","translate("+[
            config.plot_margin.left,
            config.plot_margin.top
        ]+")")

    const background = plot_g
        .append('rect')
        .attr('width', plot_width)
        .attr('height', plot_height)
        .attr('fill', '#ffffff')
        .attr('fill-opacity', '0');

    const x = d3.scaleTime()
        .range([0, plot_width])
        .domain([d3.min(data, d => d.epoch), d3.max(data, d => d.epoch)]);

    const yScaleCommits = d3.scaleLinear()
        .range([plot_height,0])
        .domain([0, d3.max(data, d => d.count)]);

    const lineCountCommits = d3.line()
        .x(d => x(d.epoch)) // xx(d.epoch))
        .y(d => yScaleCommits(d.count))
        .curve(d3.curveMonotoneX);

    const gx = svg.append("g");

    var xAxis = (g, x, height) => g
        .attr("transform","translate("+[0, height - margin.bottom] + ")")
        .call(
            d3.axisBottom(x)
                .ticks(config.svg_width / 80)
                .tickSizeOuter(0))

    // const xAxisG = plot_g
    //     .append("g")
    //     .attr('class', 'x.axis')
    //     .attr("transform","translate("+[0,plot_height] + ")")
    //     .call(d3.axisBottom(x));

    // const yAxis = ;
    const yAxisG = plot_g
        .append("g")
        .attr('class', 'y.axis')
        .call(d3.axisLeft(yScaleCommits))

    svg.append("g")
        .call(xAxis, x, svgHeight);

    const path = plot_g
        .append("path")
        .datum(data)
        .attr('class', 'line')
        .attr("d", lineCountCommits)
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

    const updateChart = (focusedArea, maxY) => {
        const focusX = x.copy().domain(focusedArea);
        const focusY = yScaleCommits.copy().domain([0, maxY]);

        // xAxis.call(xAxisG, focusX, plot_height);
        // yAxisG.call(yAxisG, focusY, data.y);
        path.attr("d", lineCountCommits);
    };

    return [svg, plot_g, x, yScaleCommits, updateChart];
}

function CommitsView({data, config, updateChart}) {
    const elementRef = useRef();
    const onUpdate = useRef();

    useEffect(() => {
        const {focusedArea, maxY} = updateChart;
        if (typeof focusedArea === 'undefined') return;
        onUpdate.current(focusedArea, maxY);
    }, [updateChart]);

    useEffect( () => {
        if (data.length === 0) return;

        const [svg, plot_g, x, y, updateChart] =
            drawChart(elementRef.current, data, config);
        onUpdate.current = updateChart;

        addMouseOver(plot_g, config, data, x, y);

        return () => {
            while (elementRef.current.firstChild) {
                elementRef.current.removeChild(elementRef.current.lastChild);
            }
        }
    }, [data, config]);

    return (
        <svg id="commits-view" ref={elementRef}></svg>
    );
}

export default CommitsView;

// https://jsfiddle.net/u7ry1dbh/