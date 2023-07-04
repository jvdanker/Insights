import React, {useEffect, useRef} from 'react';
import * as d3 from "d3";
import "./BarChart.css";
import addMouseOver from "./MouseOver";

function drawChart(ref, data, config) {
    let width = config.svg_width;
    let height = config.svg_height;
    const margin = config.plot_margin;
    const plot_width = config.svg_width - margin.left - margin.right;
    const plot_height = height - margin.top - margin.bottom;

    const svg = d3
        .select(ref)
        .attr("width", width)
        .attr("height", height)
        .attr("viewBox", [0, 0, width, height])
        .style("display", "block");

    const clip = svg.append("clipPath")
        .attr("id", "clip")
        .append("rect")
        .attr("x", margin.left)
        .attr("y", 0)
        .attr("height", height)
        .attr("width", width - margin.left - margin.right);

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
        .domain(d3.extent(data, d => d.epoch));

    const y = d3.scaleLinear()
        .range([plot_height,0])
        .domain([0, d3.max(data, d => d.count)]);

    const lineCountCommits = (x, y) =>
        d3.line()
            .x(d => x(d.epoch))
            .y(d => y(d.count))
            .curve(d3.curveMonotoneX);

    const gx = svg.append("g");

    const gy = svg.append("g");

    var xAxis = (g, x, height) => g
        .attr("transform","translate("+[0, height - margin.bottom] + ")")
        .call(d3.axisBottom(x).ticks(config.svg_width / 80).tickSizeOuter(0))

    const yAxis = (g, y) => g
        .attr("transform", `translate(${margin.left},0)`)
        .call(d3.axisLeft(y));

    const linePath = plot_g
        .append("path")
        .datum(data)
        .attr("clip-path", `url(#clip)`)
        .attr("fill", "none")
        .attr("stroke", "#707f8d");

    const selectedCircles = plot_g
        .selectAll(".myCircle")
        .data(data, d => d.epoch)
        .enter()
            .append("circle")
            .attr('class', 'myCircle')
            .attr("fill", "#5c6bce")
            .attr("stroke", "none")
            .attr("cx", function(d) { return x(d.epoch) })
            .attr("cy", function(d) { return y(d.count) })
            .attr("r", 2)
        .exit().remove();

    let update = focusedArea => {
        const [minX, maxX] = focusedArea;
        const maxY = d3.max(data, d => minX <= d.epoch && d.epoch <= maxX ? d.count : NaN);

        let xCopy = x.copy().domain(focusedArea);
        let yCopy = y.copy().domain([0, maxY]);

        gx.call(xAxis, xCopy, height);
        gy.call(yAxis, yCopy, data.y);

        linePath.attr("d", lineCountCommits(xCopy, yCopy));

        plot_g.selectAll('.myCircle')
            .attr("cx", function(d) { return xCopy(d.epoch) })
            .attr("cy", function(d) { return yCopy(d.count) })
    }

    return [plot_g, x, y, update];
}

function CommitsView({data, config, updateChart}) {
    const elementRef = useRef();
    const onUpdate = useRef();

    useEffect(() => {
        const {focusedArea} = updateChart;
        if (typeof focusedArea === 'undefined') return;
        onUpdate.current(focusedArea);
    }, [updateChart]);

    useEffect( () => {
        if (typeof data === 'undefined' || data.length === 0) return;

        const [plot_g, x, y, update] =
            drawChart(elementRef.current, data, config);
        onUpdate.current = update;

        addMouseOver(plot_g, config, data, x, y);

        return () => {
            while (elementRef.current?.firstChild) {
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