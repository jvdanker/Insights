import React, {useState} from 'react';
import * as d3 from "d3";
import CommitsView from "./CommitsView";
import {useOnce} from "./UseOnce";
import FocusView from "./FocusView";
import FocusViewBars from "./FocusViewBars";

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
    width: 800,
    height: 50,
    margin: {
        left: 30,
        right: 20,
        top: 0,
        bottom: 20,
    },
}

function BarChart() {
    const [data, setData] = useState([]);
    const [chartData, setChartData] = useState([]);
    const [updateChart, setUpdateChart] = useState({});
    const [focus, setFocus] = useState([]);
    const [committersPerDay, setCommittersPerDay] = useState([]);

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
        ;
    }

    function groupByYear() {
        // console.log(data);

        // data.map(d => {
        //     // console.log(d, d3.timeYear(d.epoch));
        // });

        const x = data.reduce((acc, curr) => {
            const key = d3.timeYear(curr.epoch).valueOf();
            // console.log(key, acc.get(key));
            return acc.set(key, (acc.get(key) || 0) + curr.count);
        }, new Map());

        const group = Array.from(
            x,
            (i) => { return {epoch: i[0], count: i[1]} });
        console.table(group);
        setChartData(group);
    }

    function updateCallback(focusedArea) {
        console.log('updateCallback', focusedArea);

        const commits = data.filter(d => focusedArea[0] < d.epoch && d.epoch < focusedArea[1]);
        console.table(commits);
        setFocus(focusedArea);

        const committersPerDayFiltered = committersPerDay.filter(d => focusedArea[0] < d.epoch && d.epoch < focusedArea[1]);
        console.table(committersPerDayFiltered);

        const daysInSelection = (focusedArea[1] - focusedArea[0]) / 86400000;
        const totalCommits = commits.reduce((acc, curr) => acc + curr.count, 0);
        const mean = d3.mean(commits, c=> c.count); // totalCommits / daysInSelection;
        const commitsDev = d3.deviation(commits, c => c.count);
        const totalFilesTouched = commits.reduce((acc, curr) => acc + curr.files, 0); // FIXME dezelfde files?
        const committers = committersPerDayFiltered.reduce((acc, curr) => acc + curr.committers.size, 0);
        const mergedCommitters = committersPerDayFiltered.reduce((acc, curr) => union(acc, curr.committers), new Set());
        console.table(mergedCommitters);

        console.log(`Selection from ${focus[0]?.toLocaleString()} to ${focus[1]?.toLocaleString()}`);
        console.log(`Selection from ${focus[0]} to ${focus[1]}`);
        console.log(`Days in selection ${daysInSelection}`);
        console.log(`Number of commits ${totalCommits}`);
        console.log(`Number of committers ${mergedCommitters.size}`);
        console.log(`Commits per day (mean) ${mean}`);
        console.log(`Commits per day (SD) ${commitsDev}`);
        console.log(`Total number of files touched ${totalFilesTouched}`);

        setUpdateChart(focusedArea);
    }

    function union(setA, setB) {
        const _union = new Set(setA);
        for (const elem of setB) {
            _union.add(elem);
        }
        return _union;
    }

    function mockData() {
        return [{
            "epoch": new Date("2022-02-01T00:00:00.000Z"),
            "count": 1,
            "committers": 1,
            "countNormalized": 1,
            "files": 3
        },{
            "epoch": new Date("2023-02-01T00:00:00.000Z"),
            "count": 2,
            "committers": 1,
            "countNormalized": 1,
            "files": 9
        },{
            "epoch": new Date("2024-02-01T00:00:00.000Z"),
            "count": 1,
            "committers": 1,
            "countNormalized": 1,
            "files": 3
        }];
    }

    function restrictData() {
        return data
            .splice(0, 100)
        ;
        // .splice(1, 2)
    }

    function decode(s) {
        let x = decodeURIComponent(s);
        return x.replaceAll('%5B', '[')
            .replaceAll('%5D', ']')
            .replaceAll('%2C', ',')
            .replaceAll('+', ' ')
            ;
    }

    useOnce( () => {
        d3.csv("committers-per-day.csv").then(data => {
            const converted = data.map(d => {
                return {
                    epoch: new Date((+d.epoch) * 86400000),
                    committers: new Set(JSON.parse(decode(d.committers)))
                }});

            setCommittersPerDay(converted);
        });

        d3.csv("commits-per-day.csv").then(data => {
            let convertedData = convertData(data);
            // convertedData = mockData();
            // convertedData = restrictData();
            // console.log(convertedData);

            setData(convertedData);
            setChartData(convertedData);
        });
    });

    function handleClick(event) {
        // console.log(event.target);
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

            <FocusViewBars
                data={data}
                config={focus_size}
                update={updateCallback}
            />

            <div>
                <div>Selection from {focus[0]?.toLocaleString()} to {focus[1]?.toLocaleString()}</div>
            </div>

            {/*<Stats data={data}/>*/}
        </>
    );
}

export default BarChart;