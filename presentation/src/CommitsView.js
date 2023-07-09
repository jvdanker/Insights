import React, {useRef} from 'react';
import * as d3 from "d3";
import {Circles} from "./Circles";

function CommitsView({data, config, focusedArea}) {
    const margin = config.margin;
    const plot_width = config.width - margin.left - margin.right;
    const plot_height = config.height - margin.top - margin.bottom;

    const [minX, maxX] = [focusedArea[0], focusedArea[1]];
    const maxY = d3.extent(data, d => minX <= d.epoch && d.epoch <= maxX ? d.count : 1)[1];

    // console.log('focusedArea', focusedArea, minX, maxX, maxY);

    const svg = d3.select('#commits-view');

    const x = d3.scaleTime()
        .domain(focusedArea)
        .range([0, plot_width])
    ;

    const y = d3.scaleLinear()
        .domain([0, maxY + 1])
        .range([plot_height + margin.top, margin.top])
    ;

    const dLine = d3.line()
        .defined(d => !isNaN(d.count) && !isNaN(d.epoch))
        .x(d => x(d.epoch))
        .y(d => y(d.count))
        .curve(d3.curveMonotoneX)
    ;

    svg.select('.gx')
        .transition()
        .duration(150)
        .call(d3.axisBottom(x).ticks().tickSizeOuter(0))
    ;

    svg.select('.gy')
        .transition()
        .duration(150)
        .call(d3.axisLeft(y))
    ;

    return (
        <svg id="commits-view"
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

                <Circles
                    config={config}
                    data={data}
                    focusedArea={focusedArea}
                />
            </g>

            <g className="gx"
               transform={`translate(${margin.left}, ${config.height - margin.bottom})`}
            />

            <g className="gy"
               transform={`translate(${margin.left}, 0)`}
            />
        </svg>
    );
}

export default CommitsView;

// https://jsfiddle.net/u7ry1dbh/