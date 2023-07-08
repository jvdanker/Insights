import React, {useRef} from 'react';
import * as d3 from "d3";
import {Circles} from "./Circles";

function CommitsView({data, config, focusedArea}) {
    const elementRef = useRef();
    const margin = config.margin;
    const plot_width = config.width - margin.left - margin.right;
    const plot_height = config.height - margin.top - margin.bottom;

    const [minX, maxX] = [focusedArea[0], focusedArea[1]];
    const maxY = d3.extent(data, d => minX <= d.epoch && d.epoch <= maxX ? d.count : 1)[1];

    // console.log('focusedArea', focusedArea, maxY);

    const x = d3.scaleTime()
        .range([0, plot_width])
        // .domain(d3.extent(data, d => d.epoch))
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

    // data.forEach(d => {
    //     console.log(d.epoch.valueOf(), d.count, x(d.epoch), y(d.count));
    // });

    d3.select('.gx') // gx
        .transition()
        .duration(150)
        .call(d3.axisBottom(x).ticks().tickSizeOuter(0))
    ;

    d3.select('.gy')
        .transition()
        .duration(150)
        .call(d3.axisLeft(y))
    ;

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