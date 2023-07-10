import * as d3 from "d3";
import React from "react";

export function Circles({config, data, focusedArea}) {
    const margin = config.margin;
    const plot_width = config.width - margin.left - margin.right;
    const plot_height = config.height - margin.top - margin.bottom;

    const [minX, maxX] = [focusedArea[0], focusedArea[1]];
    const maxY = d3.extent(data, d => minX <= d.epoch && d.epoch <= maxX ? d.count : 1)[1];

    const daysResolution = d3.timeDay.count(d3.timeYear(minX), maxX);
    // console.log('daysResolution', daysResolution);

    const strokeWidth = (daysResolution < 400) ? 2 : 0;
    const r = (daysResolution < 400) ? 3 : 2;

    const x = d3.scaleTime()
        .range([0, plot_width])
        .domain(focusedArea)
    ;

    const y = d3.scaleLinear()
        .range([plot_height + margin.top, margin.top])
        .domain([0, maxY + 1])
    ;

    const circles = data
        .filter(d => !isNaN(x(d.epoch) && !isNaN(y(d.count))))
        .map(d => {
            return {cx: x(d.epoch), cy: y(d.count)}
        })
    ;

    return (
        <>
            {circles.map(c =>
                <circle
                    key={c.cx}
                    cx={c.cx}
                    cy={c.cy}
                    r={r}
                    className="myCircle"
                    fill="#707f8d"
                    stroke="white"
                    strokeWidth={strokeWidth}
                />
            )
            }
        </>
    )
}