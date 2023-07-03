import React, {useEffect, useRef, useState} from 'react';
import * as d3 from "d3";
import "./BarChart.css";
import addMouseOver from "./MouseOver";

function drawChart(ref, data, config, update) {
    const margin = config.plot_margin;
    const plot_width = config.svg_width - margin.left - margin.right;
    const plot_height = config.svg_height - margin.top - margin.bottom;

    const x = d3.scaleTime()
        .range([0, plot_width])
        .domain([d3.min(data, d => d.epoch), d3.max(data, d => d.epoch)]);

    const y = d3.scaleLinear()
        .range([plot_height,0])
        .domain([0, d3.max(data, d => d.count)]);

    const area = (x, y) => d3.area()
        .defined(d => !isNaN(d.count))
        .x(d => x(d.epoch))
        .y0(y(0))
        .y1(d => y(d.count));

    let focusedArea = d3.extent(data, d => d.epoch);

    const xAxis = (g, x, height) => g
        .attr("transform", `translate(0, ${height})`)
        .call(d3.axisBottom(x)
            .ticks(plot_width / 80)
            .tickSizeOuter(0));

    const yAxis = (g, y) => g
        .attr("transform", `translate(${margin.left}, 0)`)
        .call(d3.axisLeft(y));
        // .call(g => g.select(".domain").remove())

    const svg = d3
        .select(ref)
        .attr("width", plot_width + margin.right + margin.left)
        .attr("height", plot_height + margin.top + margin.bottom)
        .attr("viewBox", [0, 0, config.svg_width, config.svg_height])
        .style("display", "block");

    const brush = d3
        .brushX()
        .extent([[margin.left, 0.5], [plot_width, plot_height + 0.5]])
        .on("brush", brushed)
        .on("end", brush_ended);

    const defaultSelection = [x(d3.utcDay.offset(x.domain()[1], -7)), x.range()[1]];

    svg.append("g")
        .call(xAxis, x, plot_height);

    svg.append("path")
        .datum(data)
        .attr("fill", "steelblue")
        .attr("d", area(x, y.copy().range([plot_height, 4])));

    const gb = svg.append("g")
        .call(brush)
        .call(brush.move, defaultSelection);

    function brushed({selection}) {
        console.log('brushed', selection);
        if (selection) {
            svg.property("value", selection.map(x.invert, x).map(d3.utcDay.round));
            svg.dispatch("input");
            focusedArea = svg.property('value');
            update(focusedArea);
        }
    }

    function brush_ended({selection}) {
        console.log('brush_ended', selection);
        if (!selection) {
            gb.call(brush.move, defaultSelection);
        }
    }

    return svg.node();
}

function FocusView({data, config, updateCallback}) {
    const elementRef = useRef();

    let update = (focusedArea) => {
        const [minX, maxX] = focusedArea;
        const maxY = d3.max(data, d => minX <= d.date && d.date <= maxX ? d.value : NaN);
        updateCallback(focusedArea, maxY);
    };

    useEffect( () => {
        if (data.length === 0) return;
        const svg = drawChart(elementRef.current, data, config, update);
        console.log(svg);

        // d3.brush().on('input', (a) => {
        //     console.log(a);
        // });

        return () => {
            while (elementRef.current.firstChild) {
                elementRef.current.removeChild(elementRef.current.lastChild);
            }
        }
    }, [data, config]);

    return (
        <svg ref={elementRef}></svg>
    );
}

export default FocusView;

