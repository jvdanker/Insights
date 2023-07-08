import React, {useCallback, useEffect, useRef, useState} from 'react';
import * as d3 from "d3";
import "./BarChart.css";
import addMouseOver from "./MouseOver";

function CommitsView({data, config, focusedArea}) {
    const elementRef = useRef();
    const margin = config.margin;
    const plot_width = config.width - margin.left - margin.right;
    const plot_height = config.height - margin.top - margin.bottom;

    const [minX, maxX] = [focusedArea[0], focusedArea[1]];
    const [, maxY] = d3.extent(data, d => minX <= d.epoch && d.epoch <= maxX ? d.count : NaN);
    // console.log('focusedArea', focusedArea);

    const x = d3.scaleTime()
        .range([0, plot_width])
        .domain(focusedArea)
    ;

    const y = d3.scaleLinear()
        .range([plot_height + margin.top, margin.top])
        .domain([0, maxY + 1])
    ;

    const dLine = d3.line()
        .x(d => x(d.epoch))
        .y(d => y(d.count))
        .curve(d3.curveMonotoneX)
    ;

    const xAxis = useCallback((g, x, height) => g
        .attr("transform", `translate(${margin.left}, ${height - margin.bottom})`)
        .call(d3.axisBottom(x).ticks(config.width / 80).tickSizeOuter(0))
    , [config.width, margin.bottom, margin.left]);

    const yAxis = useCallback((g, y) => g
        .attr("transform", `translate(${margin.left}, 0)`)
        .call(d3.axisLeft(y))
    , [margin.left]);

    const circles = data
        .filter(d => !isNaN(x(d.epoch) && !isNaN(y(d.count))))
        .map(d => { return { cx: x(d.epoch), cy: y(d.count) } })
    ;

    useEffect( () => {
        if (typeof data === 'undefined' || data.length === 0) return;

        d3.select('.gx') // gx
            .transition()
            .duration(450)
            .call(xAxis, x, config.height);

        d3.select('.gy') // gx
            .transition()
            .duration(450)
            .call(yAxis, y, data.y);

    }, [data, config, x, xAxis, y, yAxis]);

    return (
        <svg id="commits-view" ref={elementRef}
             width={config.width}
             height={config.height}
             viewBox={`0, 0, ${config.width}, ${config.height}`}
             display="block">

            <clipPath id="clip">
                <rect
                    x={margin.left}
                    y={margin.top}
                    width={plot_width}
                    height={plot_height} />
            </clipPath>

            <g className="plot_g" clipPath="url(#clip)">
                <path className="linePath"
                      clipPath="url(#clip)"
                      fill="none"
                      stroke="#707f8d"
                      strokeWidth={2}
                      d={dLine(data)}
                />

                {circles.map(c =>
                    <circle
                        key={c.cx}
                        cx={c.cx}
                        cy={c.cy}
                        r="4"
                        className="myCircle"
                        fill="#707f8d"
                        stroke="white"
                        strokeWidth="3"
                    />
                )}
            </g>

            <g className="gx"
               transform={`translate(${margin.left}, ${config.height - margin.bottom})`}
            />

            <g className="gy"
            />
        </svg>
    );
}

export default CommitsView;

// https://jsfiddle.net/u7ry1dbh/