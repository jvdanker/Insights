import React, {useEffect, useRef, useState} from 'react';
import * as d3 from "d3";
import "./BarChart.css";
import addMouseOver from "./MouseOver";

function drawChart(ref, data, config, update) {
    let margin = {
        top: 20,
        right: 20,
        bottom: 20,
        left: 40
    }

    let width = 800;
    let height = 50;

    const svg = d3
        .select(ref)
        .attr("width", width)
        .attr("height", height)
        .attr("viewBox", [0, 0, width, height])
        .style("display", "block");

    const brush = d3
        .brushX()
        .extent([[margin.left, 0.5], [width, height]])
        .on("brush", brushed)
        .on("end", brush_ended);

    let x = d3.scaleUtc()
        .domain(d3.extent(data, d => d.epoch))
        .range([margin.left, width - margin.right]);

    let y = d3.scaleLinear()
        .domain([0, d3.max(data, d => d.count)])
        .range([height - margin.bottom, 4]);

    let area = d3.area()
        .defined(d => !isNaN(d.count))
        .x(d => x(d.epoch))
        .y0(y(0))
        .y1(d => y(d.count * 3));

    // const defaultSelection = [x(d3.utcYear.offset(x.domain()[1], - 1)), x.range()[1]];
    const defaultSelection = [x(d3.utcDay.offset(x.domain()[1], - 5)), x.range()[1]];

    svg.append('g')
        .attr("transform", `translate(0, ${height - margin.bottom})`)
        .call(d3.axisBottom(x).ticks(width / 80).tickSizeOuter(0));

    svg.append("path")
        .datum(data)
        .attr("fill", "steelblue")
        .attr("d", area);

    const gb = svg.append("g")
        .call(brush)
        .call(brush.move, defaultSelection);

    function brushed({selection}) {
        if (selection) {
            svg.property("value", selection.map(x.invert, x).map(d3.utcDay.round));
            // svg.dispatch("input");
            update(svg.property('value'));
        }
    }

    function brush_ended({selection}) {
        if (!selection) {
            gb.call(brush.move, defaultSelection);
        }
    }

    return svg.node();
}

function FocusView(props) {
    const {data, config, update} = props;
    const elementRef = useRef();

    useEffect( () => {
        if (typeof data === 'undefined' || data.length === 0) return;
        const current = elementRef.current;

        drawChart(current, data, config, update);

        return () => {
            while (current?.firstChild) {
                current.removeChild(current.lastChild);
            }
        }
    }, [data, config]);

    return (
        <svg ref={elementRef}></svg>
    );
}

export default FocusView;

