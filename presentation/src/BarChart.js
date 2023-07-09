import React, {useState} from 'react';
import * as d3 from "d3";
import CommitsView from "./CommitsView";
import {useOnce} from "./UseOnce";
import FocusView from "./FocusView";

const config_size = {
    width: 800,
    height: 200,
    margin: {
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

function BarChart() {
    const [data, setData] = useState([]);
    const [chartData, setChartData] = useState([]);
    const [updateChart, setUpdateChart] = useState({});

    function convertData(data) {
        return data
            .map(d => {
                return {
                    epoch: new Date((+d.epoch) * 86400000),
                    count: +d.count, // Math.random(), //+d.count,
                    committers: +d.committers,
                    countNormalized: +d.count / +d.committers,
                    files: +d.files
                }
            })
            .splice(0, 10)
            .splice(1, 2)
        ;
    }

    function groupByYear() {
        console.log(data);
        console.log(d3.timeYear());

        data.map(d => {
            console.log(d, d3.timeYear(d.epoch));
        });

        console.log(d3.timeYear(2020));

        const x = data.reduce((acc, curr) => {
            const key = d3.timeYear(curr.epoch).valueOf();
            console.log(key, acc.get(key));
            return acc.set(key, (acc.get(key) || 0) + curr.count);
        }, new Map());
        console.log(x);

        const group = Array.from(
            x,
            (i) => { return {epoch: i[0], count: i[1]} });
        console.table(group);
        setChartData(group);
    }

    function updateCallback(focusedArea) {
        setUpdateChart(focusedArea);
    }

    useOnce( () => {
        d3.csv("commits-per-day.csv").then(data => {
            let convertedData = convertData(data);
            convertedData = [{
                "epoch": new Date("2007-02-07T00:00:00.000Z"),
                "count": 1,
                "committers": 1,
                "countNormalized": 1,
                "files": 3
            },{
                "epoch": new Date("2023-02-07T00:00:00.000Z"),
                "count": 1,
                "committers": 1,
                "countNormalized": 1,
                "files": 3
            }];

            console.log(convertedData);

            setData(convertedData);
            setChartData(convertedData);
        });
    });

    function handleClick(event) {
        console.log(event.target);
        if (event.target.name === 'year') {
            groupByYear();
        }
    }

    return (
        <>
            <button name="day" onClick={handleClick}>DAYS</button>
            <button name="year" onClick={handleClick}>YEARS</button>

            <CommitsView
                data={chartData}
                config={config_size}
                focusedArea={updateChart}
            />

            <FocusView
                data={data}
                config={focus_size}
                update={updateCallback}
            />

            {/*<Stats data={data}/>*/}
        </>
    );
}

export default BarChart;