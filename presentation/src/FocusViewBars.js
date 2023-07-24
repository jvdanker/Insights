import React, {useEffect, useRef, useState} from 'react';
import * as d3 from "d3";

function drawChart(ref, data, config, update) {
    const margin = config.margin;
    const plot_width = config.width - margin.left - margin.right;
    const plot_height = config.height - margin.top - margin.bottom;

    const svg = d3.select(ref);

    let [xMin, xMax] = d3.extent(data, d => d.epoch);
    xMin = d3.timeYear.floor(xMin);
    xMax = d3.timeYear.ceil(xMax);

    let x = d3.scaleTime()
        .domain([xMin, xMax])
        .range([0, plot_width]);

    const y = d3.scaleLinear()
        .domain([0, d3.max(data, d => d.count)])
        .range([config.height - margin.bottom, 4]);

    const xAxis = (g, x) => g
        .attr("transform", `translate(0, ${config.height - margin.bottom})`)
        .call(d3.axisBottom(x).ticks(config.width / 80).tickSizeOuter(0))
    ;

    const gx = svg.append('g')
        .call(xAxis, x);

    const g = svg.append("g");

    let ticks = x.ticks();
    // console.log("bars", ticks.length, plot_width);

    const bars = (data, x) => g.selectAll('.bar')
        .data(data)
        .enter().append('rect')
        .attr('class', 'bar')
        .attr('x', d => x(d.epoch))
        .attr('y', d => y(d.count * 6))
        .attr('width', 2) // (plot_width / ticks) - 3)
        .attr('height', d => plot_height - y(d.count * 6))
        .attr('fill', 'steelblue')
        // .exit().remove()
    ;

    // ***************************************************************************************************

    const defaultSelection = x => [
        x(d3.utcYear.offset(x.domain()[1], - 1)),
        // x(d3.utcDay.offset(x.domain()[1], - 5)),
        x.range()[1]
    ];

    const brush = d3.brushX()
        .extent([[0, 0], [plot_width, config.height]])
        .on("brush", brushed)
        .on("end", brush_ended);

    const gb = svg.append("g")
        .attr('class', 'g-brush')
        .call(brush)
        // .call(brush.move, defaultSelection(x))
    ;

    function brushed(event) {
        // if (event.sourceEvent && event.sourceEvent.type === "zoom") return; // ignore brush-by-zoom
        // console.log('brushed', event);

        if (event.sourceEvent && event.sourceEvent.type === 'zoom') {
            const xz = event.sourceEvent.transform.rescaleX(x);
            update(event.selection.map(xz.invert, xz).map(d3.utcDay.round));
            return;
        }

        const selection = event.selection;
        if (selection) {
            let x0 = new Date(gx.attr('domain-x0'));
            let x1 = new Date(gx.attr('domain-x1'));
            let xz = x.copy().domain([x0, x1]);

            // console.log('invert', selection.map(xz.invert, xz));
            update(selection.map(xz.invert, xz).map(d3.utcDay.round));
        }
    }

    function brush_ended(event) {
        if (event.sourceEvent && event.sourceEvent.type === "zoom") return; // ignore brush-by-zoom
        // console.log('brush_ended', event);

        const selection = event.selection;
        if (!selection) {
            gb.call(brush.move, defaultSelection(x));
        }
    }

    // ***************************************************************************************************

    const zoom = d3.zoom()
        .scaleExtent([1, 4000])
        .extent([[margin.left, 0], [config.width - margin.right, config.height]])
        .translateExtent([[margin.left, 0], [config.width - margin.right, config.height]])
        // .translateExtent([[margin.left, -Infinity], [config.width - margin.right, Infinity]])
        .on("zoom", zoomed);

    svg.call(zoom)
        .on("mousedown.zoom", null)
        .call(zoom.scaleTo, 1) // , [x(Date.UTC(2001, 8, 1)), 0]);
    ;

    function zoomed(event) {
        if (event.sourceEvent && event.sourceEvent.type === "brush") return; // ignore zoom-by-brush

        let xZoom = event.transform.rescaleX(x);
        gx.call(xAxis, xZoom);

        gx.attr('domain-x0', xZoom.domain()[0]);
        gx.attr('domain-x1', xZoom.domain()[1]);

        d3.selectAll('.bar').remove();
        bars(data, xZoom);

        let s = d3.brushSelection(gb.node());
        if (s) {
            let s3 = s.map(xZoom.invert, xZoom).map(xZoom);
            gb.call(brush.move, s3, event);
        }
    }
}

function FocusViewBars({data, config, update}) {
    const margin = config.margin;
    const elementRef = useRef();
    const [lastUpdate, setLastUpdate] = useState();
    const timer = useRef();

    useEffect(() => {
        if (typeof lastUpdate === 'undefined') return;

        if (timer.current) {
            clearTimeout(timer.current);
        }

        timer.current = setTimeout(() => {
            console.log('fire ' + lastUpdate);
            update(lastUpdate);
        }, 1000);

        return () => {
            if (timer.current) {
                clearTimeout(timer.current);
            }
        }
    }, [lastUpdate]);

    function deBounce(focus) {
        setLastUpdate(focus);
    }

    useEffect( () => {
        if (typeof data === 'undefined' || data.length === 0) return;
        const current = elementRef.current;

        drawChart(current, data, config, deBounce); // update

        return () => {
            while (current?.firstChild) {
                current.removeChild(current.lastChild);
            }
        }
    }, [data, config]);

    return (
        <>
            <svg id="focus-view-bars"
                 width={config.width}
                 height={config.height}
                 viewBox={`0, 0, ${config.width}, ${config.height}`}
                 display="block"
                 transform={`translate(${margin.left}, 0)`}
                 ref={elementRef}>
            </svg>
        </>
    );
}

export default FocusViewBars;

