import React, {useEffect, useRef} from 'react';
import * as d3 from "d3";

function drawChart(ref, data, config, update) {
    const margin = config.margin;
    const plot_width = config.width - margin.left - margin.right;

    const svg = d3.select(ref);

    const brush = d3
        .brushX()
        .extent([[0, 0], [plot_width, config.height]])
        .on("brush", brushed)
        .on("end", brush_ended);

    let [xMin, xMax] = d3.extent(data, d => d.epoch);
    xMin = d3.timeYear.floor(xMin);
    xMax = d3.timeYear.ceil(xMax);

    let x = d3.scaleUtc()
        .domain([xMin, xMax])
        .range([0, plot_width]);

    let y = d3.scaleLinear()
        .domain([0, d3.max(data, d => d.count)])
        .range([config.height - margin.bottom, 4]);

    let area = d3.area()
        .defined(d => !isNaN(d.count))
        .x(d => x(d.epoch))
        .y0(y(0))
        // .y1(d => y(d.count))
        .y1(d => y(d.count * 3))
    ;

    const defaultSelection = [
        x(d3.utcDay.offset(x.domain()[1], - 5)),
        x.range()[1]
    ];

    svg.append('g')
        // .attr("transform", `translate(${margin.left}, ${config.height - margin.bottom})`)
        .attr("transform", `translate(0, ${config.height - margin.bottom})`)
        .call(d3.axisBottom(x).ticks(config.width / 80).tickSizeOuter(0));

    svg.append("path")
        // .attr("transform", `translate(${margin.left}, 0)`)
        .attr("fill", "steelblue")
        .datum(data)
        .attr("d", area);

    const gb = svg.append("g")
        .call(brush)
        .call(brush.move, defaultSelection);

    function brushed({selection}) {
        // console.log('brushed', selection);
        if (selection) {
            update(selection.map(x.invert, x).map(d3.utcDay.round));
        }
    }

    function brush_ended({selection}) {
        // console.log('brush_ended', selection);
        if (!selection) {
            gb.call(brush.move, defaultSelection);
        }
    }
}

function FocusView({data, config, update}) {
    const margin = config.margin;
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
        <svg id="focus-view"
             width={config.width}
             height={config.height}
             viewBox={`0, 0, ${config.width}, ${config.height}`}
             display="block"
             transform={`translate(${margin.left}, 0)`}
            ref={elementRef}>

        </svg>
    );
}

export default FocusView;

