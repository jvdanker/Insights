import React, {useState} from 'react';
import * as d3 from "d3";
import "./BarChart.css";
import {Stats} from "./Stats";
import CommitsView from "./CommitsView";
import {useOnce} from "./UseOnce";
import FocusView from "./FocusView";

const config_size = {
    svg_width: 800,
    svg_height: 200,
    plot_margin: {
        left: 30,
        right: 20,
        top: 20,
        bottom: 20,
    },
}

const focus_size = {
    svg_width: 800,
    svg_height: 70,
    plot_margin: {
        left: 30,
        right: 20,
        top: 20,
        bottom: 20,
    },
}

// function updateRange(data, params) {
//     const [minX, maxX] = params;
//     const maxY = d3.max(data, d => minX <= d.epoch && d.epoch <= maxX ? d.count : NaN);
//
//     update(
//         x.copy().domain(params),
//         yScaleCommits.copy().domain([0, maxY]),
//         xAxisG,
//         yAxisG
//     );
// }
//
// function update(x, y, xAxis, yAxis) {
//     x.domain(d3.extent(data, function(d) { return d.date; }));
//     y.domain([0, d3.max(data, function(d) { return d.close; })]);
//
//     var svg = d3.select("body");
//
//     // console.log(svg.select('.line'));
//     svg.select(".line")
//         .attr("d", lineCountCommits(data));
//
//     // svg.select(".x.axis")
//     //     .call(xAxis);
//
//     // svg.select(".y.axis")
//     //     .call(yAxis);
// }

function BarChart() {
    const [data, setData] = useState([]);
    const [updateChart, setUpdateChart] = useState({});

    function convertData(data) {
        return data
            .map(d => {
                return {
                    epoch: new Date((+d.epoch) * 86400000),
                    count: +d.count,
                    committers: +d.committers,
                    countNormalized: +d.count / +d.committers,
                    files: +d.files
                }
            })
            .splice(0, 50);
    }

    function updateCallback(focusedArea, maxY) {
        console.log('callback', focusedArea, maxY);
        setUpdateChart({focusedArea, maxY});
    }

    useOnce( () => {
        d3.csv("commits-per-day.csv").then(data => {
            setData(convertData(data));
        });
    });

    return (
        <>
            <CommitsView
                data={data}
                config={config_size}
                updateChart={updateChart}
            />

            <FocusView
                data={data}
                config={focus_size}
                updateCallback={updateCallback}
            />

            {/*<Stats data={data}/>*/}
        </>
    );
}

export default BarChart;