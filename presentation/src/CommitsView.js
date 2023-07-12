import React from 'react';
import * as d3 from "d3";
import {Circles} from "./Circles";
import './CommitsView.css';

function CommitsView({data, config, focusedArea}) {
    const margin = config.margin;
    const plot_width = config.width - margin.left - margin.right;
    const plot_height = config.height - margin.top - margin.bottom;

    const [minX, maxX] = [focusedArea[0], focusedArea[1]];
    const maxY = d3.extent(data, d => minX <= d.epoch && d.epoch <= maxX ? d.count : 1)[1];

    const daysResolution = d3.timeDay.count(d3.timeYear(minX), maxX);
    const showCircles = daysResolution < 1000;
    const strokeWidth = (daysResolution < 500) ? 2 : 1;

    // console.log('focusedArea', data, focusedArea, minX, maxX, maxY);

    const svg = d3.select('#commits-view');

    const x = d3.scaleTime()
        .domain(focusedArea)
        .range([0, plot_width])
    ;

    // console.log('x', x.domain(), x.range());

    const y = d3.scaleLinear()
        .domain([0, maxY + 1])
        .range([plot_height, margin.top])
    ;

    // data.forEach(d => console.log(`d=${JSON.stringify(d)}, x=${x(d.epoch)}, y=${y(d.count)}`));

    const dLine = d3.line()
        .defined(d => !isNaN(d.count) && !isNaN(d.epoch))
        .defined(d => !isNaN(y(d.count)) && !isNaN(x(d.epoch)))
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

    const xTicks = x.ticks();
    // console.log('ticks', xTicks);
    const tickWidth = plot_width / xTicks.length;
    // console.log('tickWidth', tickWidth);

    const background = svg.select('.mouse-rect');
    const mouse_g = svg.select('.mouse-g');

    background.on("mouseover", () => mouse_g.style('display', 'block'));

    background.on("mouseout", () => mouse_g.style('display', 'none'));

    background.on("mousemove", function(event) {
        //update(selection.map(x.invert, x).map(d3.utcDay.round));

        const [x_cord] = d3.pointer(event);
        const index = d3.bisectCenter(data.map(d => d.epoch), x.invert(x_cord + margin.left)); // currentDateX);
        const current = data[index];
        const transX = x(current.epoch);

        mouse_g
            .attr('transform', `translate(${transX},${margin.top})`);

        mouse_g
            .select('text')
            .text(`${current.epoch.toLocaleDateString()}, ${current.count}`)
            .attr('text-anchor', (x_cord - margin.left) < (plot_width / 2) ? "start" : "end")
            // .attr('x', 0)
            .attr('y', 20) // y(current.count))
        ;

        mouse_g
            .select('circle')
            .attr('cy', y(current.count) - margin.top);
    });

    // ***************************************************************************************************

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

            {/*transform={`translate(${margin.left}, ${margin.top})`}*/}
            <g
                width={plot_width}
                height={plot_height}
                className="plot_g"
                clipPath="url(#clip)"
                transform={`translate(0, ${margin.top})`}
            >
                <g className="ticks">
                    {xTicks.map((t, i) =>
                        <rect
                            className="xTick"
                            x={(tickWidth * i)}
                            width={tickWidth}
                            height={plot_height}
                            fill={i%2 === 0 ? '#ffffff' : 'rgba(0,0,0,0.06)'}
                            fillOpacity={0.5}
                        />
                    )};
                </g>

                <path className="linePath"
                      // clipPath="url(#clip)"
                      fill="none"
                      stroke="steelblue"
                      strokeWidth={strokeWidth}
                      d={dLine(data)}
                />

                <g className="mouse-g" style={{display: 'block'}}>
                    <rect width={3} x={-1} height={plot_height} fill='steelblue'></rect>
                    <circle r={3} stroke="steelblue"></circle>
                    <text className="mouse-text" fontSize="10" fontFamily="sans-serif"></text>
                </g>

                <rect
                    className="mouse-rect"
                    width={plot_width}
                    height={plot_height}
                    fill="#fff"
                    fillOpacity={0}
                />

                {showCircles &&
                    <Circles config={config} data={data} focusedArea={focusedArea} />
                }
            </g>

            <g className="gx"
               transform={`translate(${margin.left}, ${config.height - margin.bottom})`}
            />

            <g className="gy"
               transform={`translate(${margin.left}, ${margin.top})`}
            />
        </svg>
    );
}

export default CommitsView;

// https://jsfiddle.net/u7ry1dbh/