import React, {useEffect, useRef, useState} from 'react';
import * as d3 from "d3";

export function Histogram({data, focusedArea}) {
    const svgRef = useRef();

    const [minX, maxX] = [focusedArea[0], focusedArea[1]];
    const newData = data.filter(d => minX <= d.epoch && d.epoch <= maxX);

    const margin = {top: 10, right: 30, bottom: 30, left: 40},
        width = 400 - margin.left - margin.right,
        height = 100 - margin.top - margin.bottom;

    const svg = d3.select('#histogram-view')
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
    ;

    // const x = d3.scaleLinear()
    //     // .domain([...Array(d3.max(newData, d => d.count)).keys()])
    //     .domain([0, d3.max(newData, d => d.count)])
    //     .range([0, width])
    //     // .paddingInner(1)
    // ;

    const max = d3.max(newData, d => d.count) || 0;
    const x = d3.scaleBand()
        .domain([...Array(max + 1).keys()])
        // .domain([0, d3.max(newData, d => d.count)])
        .rangeRound([0, width])
        // .range([0, width])
        .paddingInner(1)
    ;

    const bin = d3.bin(newData)
        .value(d => d.count)
        .domain(x.domain())
        // .thresholds(x.ticks(10))
    ;
    const bins = bin(newData); //.filter(b => x(b.x1) > x(b.x0));
console.log(x.domain(), x(1), bins);

    const y = d3.scaleLinear()
        .range([height, 0])
        .domain([0, d3.max(bins, d => d.length)])
    ;

    svg.select('.gx')
        .transition()
        .duration(150)
        .call(d3.axisBottom(x).tickSizeOuter(0).tickFormat(d3.format('.0f')))
    ;

    svg.select('.gy')
        .transition()
        .duration(150)
        .call(d3.axisLeft(y))
    ;

    svg.selectAll('.rects').selectAll("rect")
        .data(bins)
        .join("rect")
        .attr("x", 1)
        .attr("transform", (d,i) => `translate(${x(i)}, ${y(d.length)})`)
        .attr("width", x.step())
        .attr("height", d => height - y(d.length))
        .style("fill", "#0069b3a2");

    // if (data) {
    //     update(svgRef.current, data.data);
    // }

    return (
        <svg ref={svgRef} id="histogram-view">
            <g transform={`translate(${margin.left}, ${margin.top})`} className="rects"
            />

            <g className="gx"
               transform={`translate(${margin.left}, ${height + margin.top})`}
            />

            <g className="gy"
               transform={`translate(${margin.left}, ${margin.top})`}
            />
        </svg>
    )
}